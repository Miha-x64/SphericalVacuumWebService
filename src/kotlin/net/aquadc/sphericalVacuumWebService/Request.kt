package net.aquadc.sphericalVacuumWebService

/**
 * Created by miha on 01.05.17
 */

sealed class Request

data class QuadraticEquationRequest(val a: Double, val b: Double, val c: Double) : Request()

        class Example {

            // for internal use
            private var _listener: (() -> Unit)? = null

            // public set-only
            var listener: (() -> Unit)?
                @Deprecated(message = "set-only", level = DeprecationLevel.ERROR)
                get() = throw AssertionError() // unusable getter
                set(value) { _listener = value } // write-through setter

            fun somethingHappend() {
                _listener?.invoke()
            }
        }