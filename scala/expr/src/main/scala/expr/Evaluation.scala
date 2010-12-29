package expr

import scala.actors.Actor._
import Math.pow
import BinOp._

object Evaluation {
  def eval(expression: Expr, env: Env): Double = {
    def e(expr: Expr) = eval(expr, env)

    expression match {
      case Num(x) => x
      case Name(x) => env.variable(x)
      case Call(name, args) => env.function(name).eval(env, args.map(e))
      case x + y => e(x) + e(y)
      case x - y => e(x) - e(y)
      case x * y => e(x) * e(y)
      case x / y => e(x) / e(y)
      case x ^ y => pow(e(x), e(y))
      case _ => error("Cannot evaluate expression: " + expression)
    }
  }

  def ceval(expression: Expr, env: Env): Double = {
    def e(expr: Expr) = ceval(expr, env)

    def paralelize(leftExpr: Expr, rightExpr: Expr)(fun: (Double, Double) => Double): Double = {
      var left, right: Double = 0
      val caller = self

      actor { caller ! ("left", e(leftExpr)) }
      actor { caller ! ("right", e(rightExpr)) }

      for (i <- 1 to 2) {
        receive {
          case ("left", value: Double) => left = value
          case ("right", value: Double) => right = value
        }
      }

      fun(left, right)
    }

    expression match {
      case Num(x) => x
      case Name(x) => env.variable(x)
      case Call(name, args) => env.function(name).eval(env, args.map(e))
      case x + y => paralelize(x, y) { _ + _ }
      case x - y => paralelize(x, y) { _ - _ }
      case x * y => paralelize(x, y) { _ * _ }
      case x / y => paralelize(x, y) { _ / _ }
      case x ^ y => paralelize(x, y) { pow(_, _) }
      case _ => error("Cannot evaluate expression: " + expression)
    }
  }
}
