package im.molly.monero.internal

/**
 * Annotation that shows the method is called by JNI native code.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class CalledByNative
