package monocle.law.discipline

import cats.Eq
import monocle.Lens
import monocle.law.LensLaws
import org.scalacheck.Arbitrary
import org.scalacheck.Prop._
import org.typelevel.discipline.Laws

object LensTests extends Laws {

  def apply[S: Arbitrary : Eq, A: Arbitrary : Eq](lens: Lens[S, A]): RuleSet = {
    val laws: LensLaws[S, A] = new LensLaws(lens)
    new SimpleRuleSet("Lens",
      "set what you get" -> forAll( (s: S) => laws.getSet(s)),
      "get what you set" -> forAll( (s: S, a: A) => laws.setGet(s, a)),
      "set idempotent"   -> forAll( (s: S, a: A) => laws.setIdempotent(s, a)),
      "modify id = id"   -> forAll( (s: S) => laws.modifyIdentity(s)),
      "modifyF Id = Id"  -> forAll( (s: S) => laws.modifyFId(s))
    )
  }

}
