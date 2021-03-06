package maml.tile

import geotrellis.raster._


case class Classify(children: Array[LazyTile], f: Double => Int) extends LazyTile.UnaryBranch {
  def get(col: Int, row: Int) = f(fst.getDouble(col, row))
  def getDouble(col: Int, row: Int) = i2d(get(col, row))
}
