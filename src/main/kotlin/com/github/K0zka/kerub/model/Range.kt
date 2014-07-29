package com.github.K0zka.kerub.model

import javax.xml.bind.annotation.XmlRootElement
import java.io.Serializable

XmlRootElement(name = "range")
data class Range<T> (final val min : T, final val max: T) : Serializable {
}