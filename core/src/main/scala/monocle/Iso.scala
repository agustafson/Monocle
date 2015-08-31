package monocle

import cats.{Applicative, Functor, Monoid}
import cats.arrow.{Category, Split}
import cats.data.Xor

/**
 * A [[PIso]] defines an isomorphism between types S, A and B, T:
 * <pre>
 *              get                           reverse.get
 *     -------------------->             -------------------->
 *   S                       A         T                       B
 *     <--------------------             <--------------------
 *       reverse.reverseGet                   reverseGet
 * </pre>
 *
 * In addition, if f and g forms an isomorphism between `A` and `B`, i.e. if `f . g = id` and `g . f = id`,
 * then a [[PIso]] defines an isomorphism between `S` and `T`:
 * <pre>
 *     S           T                                   S           T
 *     |           ↑                                   ↑           |
 *     |           |                                   |           |
 * get |           | reverseGet     reverse.reverseGet |           | reverse.get
 *     |           |                                   |           |
 *     ↓     f     |                                   |     g     ↓
 *     A --------> B                                   A <-------- B
 * </pre>
 *
 * [[Iso]] is a type alias for [[PIso]] where `S` = `A` and `T` = `B`:
 * {{{
 * type Iso[S, A] = PIso[S, S, A, A]
 * }}}
 *
 * A [[PIso]] is also a valid [[Getter]], [[Fold]], [[PLens]], [[PPrism]], [[POptional]], [[PTraversal]] and [[PSetter]]
 *
 * @see [[monocle.law.IsoLaws]]
 *
 * @tparam S the source of a [[PIso]]
 * @tparam T the modified source of a [[PIso]]
 * @tparam A the target of a [[PIso]]
 * @tparam B the modified target of a [[PIso]]
 */
abstract class PIso[S, T, A, B] extends Serializable { self =>

  /** get the target of a [[PIso]] */
  def get(s: S): A

  /** get the modified source of a [[PIso]] */
  def reverseGet(b: B): T

  /** reverse a [[PIso]]: the source becomes the target and the target becomes the source */
  def reverse: PIso[B, A, T, S]

  /** modify polymorphically the target of a [[PIso]] with a Functor function */
  final def modifyF[F[_]](f: A => F[B])(s: S)(implicit F: Functor[F]): F[T] =
    Functor[F].map(f(get(s)))(reverseGet)

  /** modify polymorphically the target of a [[PIso]] with a function */
  final def modify(f: A => B): S => T =
    s => reverseGet(f(get(s)))

  /** set polymorphically the target of a [[PIso]] with a value */
  final def set(b: B): S => T =
    _ => reverseGet(b)

  /** pair two disjoint [[PIso]] */
  final def product[S1, T1, A1, B1](other: PIso[S1, T1, A1, B1]): PIso[(S, S1), (T, T1), (A, A1), (B, B1)] =
    PIso[(S, S1), (T, T1), (A, A1), (B, B1)]{
      case (s, s1) => (get(s), other.get(s1))
    }{
      case (b, b1) => (reverseGet(b), other.reverseGet(b1))
    }

  final def first[C]: PIso[(S, C), (T, C), (A, C), (B, C)] =
    PIso[(S, C), (T, C), (A, C), (B, C)]{
      case (s, c) => (get(s), c)
    }{
      case (b, c) => (reverseGet(b), c)
    }

  final def second[C]: PIso[(C, S), (C, T), (C, A), (C, B)] =
    PIso[(C, S), (C, T), (C, A), (C, B)]{
      case (c, s) => (c, get(s))
    }{
      case (c, b) => (c, reverseGet(b))
    }

  /**********************************************************/
  /** Compose methods between a [[PIso]] and another Optics */
  /**********************************************************/

  /** compose a [[PIso]] with a [[Fold]] */
  final def composeFold[C](other: Fold[A, C]): Fold[S, C] =
    asFold composeFold other

  /** compose a [[PIso]] with a [[Getter]] */
  final def composeGetter[C](other: Getter[A, C]): Getter[S, C] =
    asGetter composeGetter other

  /** compose a [[PIso]] with a [[PSetter]] */
  final def composeSetter[C, D](other: PSetter[A, B, C, D]): PSetter[S, T, C, D] =
    asSetter composeSetter other

  /** compose a [[PIso]] with a [[PTraversal]] */
  final def composeTraversal[C, D](other: PTraversal[A, B, C, D]): PTraversal[S, T, C, D] =
    asTraversal composeTraversal other

  /** compose a [[PIso]] with a [[POptional]] */
  final def composeOptional[C, D](other: POptional[A, B, C, D]): POptional[S, T, C, D] =
    asOptional composeOptional other

  /** compose a [[PIso]] with a [[PPrism]] */
  final def composePrism[C, D](other: PPrism[A, B, C, D]): PPrism[S, T, C, D] =
    asPrism composePrism other

  /** compose a [[PIso]] with a [[PLens]] */
  final def composeLens[C, D](other: PLens[A, B, C, D]): PLens[S, T, C, D] =
    asLens composeLens other

  /** compose a [[PIso]] with a [[PIso]] */
  final def composeIso[C, D](other: PIso[A, B, C, D]): PIso[S, T, C, D] =
    new PIso[S, T, C, D]{ composeSelf =>
      def get(s: S): C =
        other.get(self.get(s))

      def reverseGet(d: D): T =
        self.reverseGet(other.reverseGet(d))

      def reverse: PIso[D, C, T, S] =
        new PIso[D, C, T, S]{
          def get(d: D): T =
            self.reverseGet(other.reverseGet(d))

          def reverseGet(s: S): C =
            other.get(self.get(s))

          def reverse: PIso[S, T, C, D] =
            composeSelf
        }
    }

  /********************************************/
  /** Experimental aliases of compose methods */
  /********************************************/

  /** alias to composeTraversal */
  final def ^|->>[C, D](other: PTraversal[A, B, C, D]): PTraversal[S, T, C, D] =
    composeTraversal(other)

  /** alias to composeOptional */
  final def ^|-?[C, D](other: POptional[A, B, C, D]): POptional[S, T, C, D] =
    composeOptional(other)

  /** alias to composePrism */
  final def ^<-?[C, D](other: PPrism[A, B, C, D]): PPrism[S, T, C, D] =
    composePrism(other)

  /** alias to composeLens */
  final def ^|->[C, D](other: PLens[A, B, C, D]): PLens[S, T, C, D] =
    composeLens(other)

  /** alias to composeIso */
  final def ^<->[C, D](other: PIso[A, B, C, D]): PIso[S, T, C, D] =
    composeIso(other)

  /****************************************************************/
  /** Transformation methods to view a [[PIso]] as another Optics */
  /****************************************************************/

  /** view a [[PIso]] as a [[Fold]] */
  final def asFold: Fold[S, A] =
    new Fold[S, A]{
      def foldMap[M: Monoid](f: A => M)(s: S): M =
        f(get(s))
    }

  /** view a [[PIso]] as a [[Getter]] */
  final def asGetter: Getter[S, A] =
    new Getter[S, A]{
      def get(s: S): A =
        self.get(s)
    }

  /** view a [[PIso]] as a [[Setter]] */
  final def asSetter: PSetter[S, T, A, B] =
    new PSetter[S, T, A, B]{
      def modify(f: A => B): S => T =
        self.modify(f)

      def set(b: B): S => T =
        self.set(b)
    }

  /** view a [[PIso]] as a [[PTraversal]] */
  final def asTraversal: PTraversal[S, T, A, B] =
    new PTraversal[S, T, A, B] {
      def modifyF[F[_]: Applicative](f: A => F[B])(s: S): F[T] =
        self.modifyF(f)(s)
    }

  /** view a [[PIso]] as a [[POptional]] */
  final def asOptional: POptional[S, T, A, B] =
    new POptional[S, T, A, B]{
      def getOrModify(s: S): T Xor A =
        Xor.right(get(s))

      def set(b: B): S => T =
        self.set(b)

      def getOption(s: S): Option[A] =
        Some(self.get(s))

      def modify(f: A => B): S => T =
        self.modify(f)

      def modifyF[F[_]: Applicative](f: A => F[B])(s: S): F[T] =
        self.modifyF(f)(s)
    }

  /** view a [[PIso]] as a [[PPrism]] */
  final def asPrism: PPrism[S, T, A, B] =
    new PPrism[S, T, A, B]{
      def getOrModify(s: S): T Xor A =
        Xor.right(get(s))

      def reverseGet(b: B): T =
        self.reverseGet(b)

      def getOption(s: S): Option[A] =
        Some(self.get(s))
    }

  /** view a [[PIso]] as a [[PLens]] */
  final def asLens: PLens[S, T, A, B] =
    new PLens[S, T, A, B]{
      def get(s: S): A =
        self.get(s)

      def set(b: B): S => T =
        self.set(b)

      def modify(f: A => B): S => T =
        self.modify(f)

      def modifyF[F[_]: Functor](f: A => F[B])(s: S): F[T] =
        self.modifyF(f)(s)
    }

}

object PIso extends IsoInstances {
  /** create a [[PIso]] using a pair of functions: one to get the target and one to get the source. */
  def apply[S, T, A, B](_get: S => A)(_reverseGet: B => T): PIso[S, T, A, B] =
    new PIso[S, T, A, B]{ self =>
      def get(s: S): A =
        _get(s)

      def reverseGet(b: B): T =
        _reverseGet(b)

      def reverse: PIso[B, A, T, S] =
        new PIso[B, A, T, S] {
          def get(b: B): T =
            _reverseGet(b)

          def reverseGet(s: S): A =
            _get(s)

          def reverse: PIso[S, T, A, B] =
            self
        }
    }

  /**
   * create a [[PIso]] between any type and itself. id is the zero element of optics composition,
   * for all optics o of type O (e.g. Lens, Iso, Prism, ...):
   * o      composeIso Iso.id == o
   * Iso.id composeO   o        == o (replace composeO by composeLens, composeIso, composePrism, ...)
   */
  def id[S, T]: PIso[S, T, S, T] =
    new PIso[S, T, S, T] { self =>
      def get(s: S): S = s
      def reverseGet(t: T): T = t
      def reverse: PIso[T, S, T, S] =
        new PIso[T, S, T, S] {
          def get(t: T): T = t
          def reverseGet(s: S): S = s
          def reverse: PIso[S, T, S, T] = self
        }
    }
}

object Iso {
  /** alias for [[PIso]] apply when S = T and A = B */
  def apply[S, A](get: S => A)(reverseGet: A => S): Iso[S, A] =
    PIso(get)(reverseGet)

  /** alias for [[PIso]] id when S = T and A = B */
  def id[S]: Iso[S, S] =
    PIso.id[S, S]
}

sealed abstract class IsoInstances extends IsoInstances0 {
  implicit val isoSplit: Split[Iso] = new Split[Iso] {
    def split[A, B, C, D](f: Iso[A, B], g: Iso[C, D]): Iso[(A, C), (B, D)] =
      f product g

    def compose[A, B, C](f: Iso[B, C], g: Iso[A, B]): Iso[A, C] =
      g composeIso f
  }
}

sealed abstract class IsoInstances0 {
  implicit val isoCategory: Category[Iso] = new Category[Iso]{
    def id[A]: Iso[A, A] =
      Iso.id[A]

    def compose[A, B, C](f: Iso[B, C], g: Iso[A, B]): Iso[A, C] =
      g composeIso f
  }
}