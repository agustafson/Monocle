package monocle.interopscalaz

import monocle.function.{Each, Index, _}
import monocle.{Iso, Optional, PIso, Prism}

import scalaz.Id.Id
import scalaz._
import scalaz.std.option._
import scalaz.syntax.traverse._

object ilist extends IListOptics

trait IListOptics {

  final def pIListToList[A, B]: PIso[IList[A], IList[B], List[A], List[B]] =
    PIso[IList[A], IList[B], List[A], List[B]](_.toList)(IList.fromList)

  final def iListToList[A]: Iso[IList[A], List[A]] =
    pIListToList[A, A]

  implicit def iListEmpty[A]: Empty[IList[A]] = new Empty[IList[A]] {
    def empty = Prism[IList[A], Unit](l => if(l.isEmpty) Some(()) else None)(_ => IList.empty)
  }

  implicit def iNilEmpty[A]: Empty[INil[A]] = new Empty[INil[A]] {
    def empty = Prism[INil[A], Unit](_ => Some(()))(_ => INil())
  }

  implicit def iListEach[A]: Each[IList[A], A] = Each.traverseEach[IList, A](typeclass.traverse.get(Traverse[IList]))

  implicit def iListIndex[A]: Index[IList[A], Int, A] = new Index[IList[A], Int, A] {
    def index(i: Int) = Optional[IList[A], A](
      il      => if(i < 0) None else il.drop(i).headOption)(
      a => il => il.zipWithIndex.traverse[Id, A]{
        case (_    , index) if index == i => a
        case (value, index)               => value
      }
    )
  }

  implicit def iListFilterIndex[A]: FilterIndex[IList[A], Int, A] =
    FilterIndex.traverseFilterIndex[IList, A](_.zipWithIndex)(typeclass.traverse.get(Traverse[IList]))

  implicit def iListCons[A]: Cons[IList[A], A] = new Cons[IList[A], A]{
    def cons = Prism[IList[A], (A, IList[A])]{
      case INil()       => None
      case ICons(x, xs) => Some((x, xs))
    }{ case (a, s) => ICons(a, s) }
  }

  implicit def iListSnoc[A]: Snoc[IList[A], A] = new Snoc[IList[A], A]{
    def snoc = Prism[IList[A], (IList[A], A)](
      il => Applicative[Option].apply2(il.initOption, il.lastOption)((_,_))){
      case (init, last) => init :+ last
    }
  }

  implicit def iListReverse[A]: Reverse[IList[A], IList[A]] =
    reverseFromReverseFunction[IList[A]](_.reverse)

}
