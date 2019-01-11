# Interpreter Case Study

WIP: Case study building a mini interpreter for a simple DSL.

1. Untyped DSLs

a. Build a simple DSL representing:
   numbers, addition, and multiplication

i. Build an ADT `Expr` representing an expression

```scala
// 1 * 2 + 3 * 4
val expr: Expr = Add(Mul(Lit(1), Lit(2)),
                     Mul(Lit(3), Lit(4)))
```

ii. Build an interpreter to run expressions:

```scala
def eval(expr: Expr): Int
```

iii. Make your interpreter generic in some `F[_]`
     (hint: what is F? a monad? a functor? an applicative?
      this will change from exercise to exercise)

b. Add some features that require error handling:
   - booleans as well as numbers
   - boolean operators (<= and && at minimum)

i. Add some terms to your `Expr` type

```scala
// 1 < 2 && 3 < 4
// And(Lt(Lit(1), Lit(2)), Lt(Lit(3), Lit(4)))
// Lt(Lt(Lit(1), Lit(2)), Lt(Lit(3), Lit(4)))
```

ii. Update your interpreter to handle the new types
    - how to represent the different types
      expressions can calculate?
      - Value type
    - what kinds of errors can happen now?
      - try to treat Value as Int / Boolean
    - the interpreter will need some error handling
      - ApplicativeError or MonadError

```scala
class Interpreter[F[_]: ApplicativeError[?[_], String]] {
  def eval(expr: Expr): F[Value] = ???

  // def asInt(value: Value): F[Int] = ???
  // def asBoolean(value: Value): F[Boolean] = ???

  def evalAsInt(expr: Expr): F[Int] =
    eval(expr).flatMap {
      case IntVal(i)  => i.pure[F]
      case BoolVal(b) => s"Expected integer, received $b".raiseError[F, Int]
    }

  def evalAsBool(expr: Expr): F[Boolean] =
    eval(expr).flatMap {
      case IntVal(i)  => s"Expected boolean, received $i".raiseError[F, Boolean]
      case BoolVal(b) => b.pure[F]
    }
}
```

c. If you have time, add some more interesting expression types:
   - conditionals
   - logging statements
   - blocks

## Aside on kind projector and type lambdas

```scala
type ErrorOr[A] = Either[String, A]
Monad[ErrorOr]

Monad[Either[String, ?]]

Monad[({ type ErrorOr[A] = Either[String, A] })#ErrorOr]

type AsyncErrorOr[A] = EitherT[Future, String, A]

MonadError[Future, Throwable]
MonadError[ErrorOr, String]
MonadError[AsyncErrorOr, String]
```

# Typed DSLs

The interpreter is probably quite complex by now.
We can make it simpler using Scala's type system.

a. Convert your untyped DSL to a typed DSL

i. Add a type parameter to `Expr`:

```scala
sealed trait Expr[A]
```

ii. Your interpreter now becomes:

```scala
def eval[F[_], A](expr: Expr[A]): F[A]
```

The interpreter should need less error handling.
Can you downgrade from an `(Applicative|Monad)Error`
to an `(Applicative|Monad)`?

# Monadic DSLs

So far, the interpreter has dictated
the order in which statements are executed.
We can hand this control over to the user
if we make `Expr` a *monad*.

a. Add a new type of `Expr` called `FlatMapExpr`.

```scala
// The flatMap method on a Monad looks like this:
new Monad[Expr] {
  def flatMap[A, B](fa: Expr[A])(f: A => Expr[B]): Expr[B]
}

// We *reify* that to a data structure
// so we can evaluate it in our interpreter later:
case class FlatMapExpr[A, B](fa: Expr[A], f: A => Expr[B]) extends Expr[B]
```

b. Create `map` and `flatMap` methods for `Expr`,
   either by writing them directly
   or by creating a `Monad` instance for `Expr`.

c. We now have `FlatMapExpr` to specify ordering.
   We don't need to nest other types of `Expr`:

```scala
// Your existing case class
case class Add(l: Expr[Int], r: Expr[Int]) extends Expr[Int]

// Can be rewritten as something simpler:
case class Add(l: Int, r: Int) extends Expr[Int]
```

d. Any programs in the DSL now have to be written
   as for comprehensions:

```scala
for {
  a <- Lit(1) : Expr[Int]
  b <- Lit(2)
  c <- Add(a, b)
} yield c
```

# Tagless DSL/Interpreter

```scala
trait Expr[F[_]] {
  def add(x: Int, y: Int): F[Int]
  def mul(x: Int, y: Int): F[Int]
  def lt(x: Int, y: Int): F[Boolean]
  def and(x: Boolean, y: Boolean): F[Boolean]
}

trait KeyValueStore[F[_]] {
  def get(key: String): F[Int]
  def set(key: String, value: Int): F[Unit]
}

class Program[F[_], G[_]](expr: Expr[F], store: KeyValueStore[G])(
  implicit
  monad: Monad[F],
  transform: F ~> G
) {
  import expr._
  import store._

  def run: F[Int] =
    for {
      a <- transform(1.pure[F])
      b <- transform(2.pure[F])
      c <- transform(add(a, b))
      _ <- set("temp", c)
      d <- get("temp")
    } yield d
}
```
