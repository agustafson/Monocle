package monocle.law.discipline

import cats.Eq
import cats.std.option._
import monocle.Prism
import monocle.law.PrismLaws
import org.scalacheck.Arbitrary
import org.scalacheck.Prop._
import org.typelevel.discipline.Laws

object PrismTests extends Laws {

  def apply[S: Arbitrary : Eq, A: Arbitrary : Eq](prism: Prism[S, A]): RuleSet = {
    val laws: PrismLaws[S, A] = new PrismLaws(prism)
    new SimpleRuleSet("Prism",
      "partial round trip one way" -> forAll( (s: S) => laws.partialRoundTripOneWay(s)),
      "round trip other way" -> forAll( (a: A) => laws.roundTripOtherWay(a)),
      "modify id = id"       -> forAll( (s: S) => laws.modifyIdentity(s)),
      "modifyF Id = Id"      -> forAll( (s: S) => laws.modifyFId(s)),
      "modifyOption"         -> forAll( (s: S) => laws.modifyOptionIdentity(s))
    )
  }

}
