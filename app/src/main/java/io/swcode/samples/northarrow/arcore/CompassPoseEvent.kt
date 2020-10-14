package io.swcode.samples.northarrow.arcore

import com.google.ar.core.Frame
import com.google.ar.core.Pose

data class CompassPoseEvent(val pose: Pose, val frame: Frame)