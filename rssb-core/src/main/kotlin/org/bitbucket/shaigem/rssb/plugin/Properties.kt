/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 * Copyright by contributors of https://github.com/edvin/tornadofx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package org.bitbucket.shaigem.rssb.plugin

import javafx.beans.property.*
import javafx.beans.value.*
import java.lang.reflect.Field
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

fun <T> property(value: T? = null) = PropertyDelegate(SimpleObjectProperty<T>(value))
fun <T> property(block: () -> Property<T>) = PropertyDelegate(block())

fun <T> Any.getProperty(prop: KMutableProperty1<*, T>): ObjectProperty<T> {
    // avoid kotlin-reflect dependency
    val field = javaClass.findFieldByName("${prop.name}\$delegate")
            ?: throw IllegalArgumentException("No delegate field with name '${prop.name}' found")

    field.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val delegate = field.get(this) as PropertyDelegate<T>
    return delegate.fxProperty as ObjectProperty<T>
}

fun Class<*>.findFieldByName(name: String): Field? {
    val field = (declaredFields + fields).find { it.name == name }
    if (field != null) return field
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    if (superclass == java.lang.Object::class.java) return null
    return superclass.findFieldByName(name)
}


class PropertyDelegate<T>(val fxProperty: Property<T>) : ReadWriteProperty<Any, T> {

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        return fxProperty.value
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        fxProperty.value = value
    }
}

/**
 * Convert an owner instance and a corresponding property reference into an observable
 */
fun <S, T> S.observable(prop: KMutableProperty1<S, T>): ObjectProperty<T> {
    val owner = this
    return object : SimpleObjectProperty<T>(owner, prop.name) {
        override fun get() = prop.get(owner)
        override fun set(v: T) = prop.set(owner, v)
    }
}

/**
 * Convert an owner instance and a corresponding property reference into a readonly observable
 */
fun <S, T> observable(owner: S, prop: KProperty1<S, T>): ReadOnlyObjectProperty<T> {
    return object : ReadOnlyObjectWrapper<T>(owner, prop.name) {
        override fun get() = prop.get(owner)
    }
}


operator fun <T> ObservableValue<T>.getValue(thisRef: Any, property: KProperty<*>) = value
operator fun <T> Property<T?>.setValue(thisRef: Any, property: KProperty<*>, value: T?) {
    this.value = value
}

operator fun ObservableDoubleValue.getValue(thisRef: Any, property: KProperty<*>): Double = value.toDouble()
operator fun DoubleProperty.setValue(thisRef: Any, property: KProperty<*>, value: Double) {
    this.value = value
}

operator fun ObservableFloatValue.getValue(thisRef: Any, property: KProperty<*>): Float = value.toFloat()
operator fun FloatProperty.setValue(thisRef: Any, property: KProperty<*>, value: Float) {
    this.value = value
}

operator fun ObservableLongValue.getValue(thisRef: Any, property: KProperty<*>): Long = value.toLong()
operator fun LongProperty.setValue(thisRef: Any, property: KProperty<*>, value: Long) {
    this.value = value
}

operator fun ObservableIntegerValue.getValue(thisRef: Any, property: KProperty<*>): Int = value.toInt()
operator fun IntegerProperty.setValue(thisRef: Any, property: KProperty<*>, value: Int) {
    this.value = value
}

operator fun ObservableBooleanValue.getValue(thisRef: Any, property: KProperty<*>): Boolean = value
operator fun BooleanProperty.setValue(thisRef: Any, property: KProperty<*>, value: Boolean) {
    this.value = value
}

