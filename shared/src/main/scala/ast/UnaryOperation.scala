package maml.ast

import maml.ast.metadata._
import maml.ast.utility._

import java.util.UUID

/** Operations which should only have one argument. */
trait UnaryOperation extends Operation with Product with Serializable {
  val symbol: String
}

case class Classification(args: List[MamlTree], id: UUID, metadata: Option[NodeMetadata], classMap: ClassMap)
    extends UnaryOperation {
  val symbol: String = "classify"
  def withArgs(newArgs: List[MamlTree]): MamlTree = copy(args = newArgs)
}

case class Masking(args: List[MamlTree], id: UUID, metadata: Option[NodeMetadata], mask: MultiPolygon)
    extends UnaryOperation {
  val symbol: String = "mask"
  def withArgs(newArgs: List[MamlTree]): MamlTree = copy(args = newArgs)
}
