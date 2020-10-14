package io.swcode.samples.northarrow.math

import com.google.ar.core.Pose

fun Pose.rotationToQuaternion(): Quaternion {
    return Quaternion(
        rotationQuaternion[0],
        rotationQuaternion[1],
        rotationQuaternion[2],
        rotationQuaternion[3]
    )
}

fun Pose.translationToVector3(): Vector3 {
    return Vector3(
        translation[0],
        translation[1],
        translation[2]
    )
}