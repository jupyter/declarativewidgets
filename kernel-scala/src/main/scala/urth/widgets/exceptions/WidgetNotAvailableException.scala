/**
 * Copyright (c) Jupyter Development Team.
 * Distributed under the terms of the Modified BSD License.
 */

package urth.widgets.exceptions

case class WidgetNotAvailableException(klass: String)
  extends RuntimeException(s"Scala widget class for $klass not found!")
