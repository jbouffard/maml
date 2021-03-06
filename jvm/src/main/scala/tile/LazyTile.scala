package maml.tile

import com.typesafe.scalalogging.LazyLogging
import geotrellis.raster._
import geotrellis.raster.mapalgebra.local._
import geotrellis.raster.mapalgebra.focal
import geotrellis.raster.mapalgebra.focal.Neighborhood
import geotrellis.raster.render._
import geotrellis.vector.{ Extent, MultiPolygon, Point }
import spire.syntax.cfor._


sealed trait LazyTile extends LazyLogging {
  def children: Array[LazyTile]
  def cols: Int
  def rows: Int
  def get(col: Int, row: Int): Int
  def getDouble(col: Int, row: Int): Double

  def evaluateAs(ct: CellType): Tile = {
    val mutableOutput = ArrayTile.empty(ct, cols, rows)
    if (ct.isFloatingPoint) {
      cfor(0)(_ < rows, _ + 1) { row =>
        cfor(0)(_ < cols, _ + 1) { col =>
          mutableOutput.setDouble(col, row, getDouble(col, row))
        }
      }
    } else {
      cfor(0)(_ < rows, _ + 1) { row =>
        cfor(0)(_ < cols, _ + 1) { col =>
          mutableOutput.set(col, row, get(col, row))
        }
      }
    }
    mutableOutput
  }

  def evaluate = evaluateAs(IntConstantNoDataCellType)

  def evaluateDouble = evaluateAs(DoubleConstantNoDataCellType)

}

object LazyTile {

  def apply(tile: Tile): LazyTile = Bound(tile)

  /** An LazyTile.Tree has a left and right. The terminal node will have Nil on the left and right */
  trait Branch extends LazyTile {
    lazy val cols = {
      val colList = children.map(_.cols).distinct
      require(colList.length == 1, "Ambiguous column count")
      colList.head
    }
    lazy val rows = {
      val rowList = children.map(_.cols).distinct
      require(rowList.length == 1, "Ambiguous row count")
      rowList.head
    }
  }

  trait BranchWithArity extends Branch {
    val arity: Int
    require(children.length == arity, s"Incorrect arity: $arity argument expected, ${children.length} found")
  }

  trait UnaryBranch extends BranchWithArity {
    val arity = 1
    def fst = children.head
  }

  trait BinaryBranch extends BranchWithArity {
    val arity = 2
    def fst = children.head
    def snd = children.head
  }

  trait Terminal extends LazyTile {
    def children: Array[LazyTile]
  }

  /** This object represents tile data sources */
  case class Bound(tile: Tile) extends Terminal {
    def children: Array[LazyTile] = Array.empty
    def cols: Int = tile.cols
    def rows: Int = tile.rows
    def get(col: Int, row: Int): Int = tile.get(col,row)
    def getDouble(col: Int, row: Int): Double = tile.getDouble(col, row)
  }

  /* --- Mapping --- */
  case class MapInt(children: Array[LazyTile], f: Int => Int) extends UnaryBranch {
    def get(col: Int, row: Int) = f(fst.get(col, row))
    def getDouble(col: Int, row: Int) = i2d(fst.get(col, row))
  }

  case class MapDouble(children: Array[LazyTile], f: Double => Double) extends UnaryBranch {
    def get(col: Int, row: Int) = d2i(f(fst.getDouble(col, row)))
    def getDouble(col: Int, row: Int) = f(fst.getDouble(col, row))
  }

  case class DualMap(children: Array[LazyTile], f: Int => Int, g: Double => Double) extends UnaryBranch {
    def get(col: Int, row: Int) = f(fst.get(col, row))
    def getDouble(col: Int, row: Int) = g(fst.getDouble(col, row))
  }

  /* --- Combining --- */
  case class CombineInt(children: Array[LazyTile], f: (Int, Int) => Int) extends BinaryBranch {
    def get(col: Int, row: Int) = f(fst.get(col, row), snd.get(col, row))
    def getDouble(col: Int, row: Int) = i2d(f(fst.get(col, row), snd.get(col, row)))
  }

  case class CombineDouble(children: Array[LazyTile], f: (Double, Double) => Double) extends BinaryBranch {
    def get(col: Int, row: Int) = d2i(f(fst.getDouble(col, row), snd.getDouble(col, row)))
    def getDouble(col: Int, row: Int) = f(fst.getDouble(col, row), snd.getDouble(col, row))
  }

  case class DualCombine(children: Array[LazyTile], f: (Int, Int) => Int, g: (Double, Double) => Double) extends BinaryBranch {
    def get(col: Int, row: Int) = f(fst.get(col, row), snd.get(col, row))
    def getDouble(col: Int, row: Int) = g(fst.getDouble(col, row), snd.getDouble(col, row))
  }
}
