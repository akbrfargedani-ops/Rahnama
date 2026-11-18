package com.example.ui.map

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.RoundaboutLeft
import androidx.compose.material.icons.rounded.Straight
import androidx.compose.material.icons.rounded.TurnLeft
import androidx.compose.material.icons.rounded.TurnRight
import androidx.compose.material.icons.rounded.UTurnLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.DistanceUnit
import com.example.ui.theme.RahnamaEmerald
import com.example.ui.viewmodel.RoutingUiState
import com.example.util.GeoUtils

@Composable
fun NavigationHud(
    routingState: RoutingUiState,
    distanceUnit: DistanceUnit,
    speedMps: Float,
    voiceEnabled: Boolean,
    onToggleVoice: () -> Unit,
    onPrevStep: () -> Unit,
    onNextStep: () -> Unit,
    onStopNavigation: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onStopNavigation)

    val route = routingState.selectedRoute
    val currentStep = route?.steps?.getOrNull(routingState.currentStepIndex)
    val nextStep = route?.steps?.getOrNull(routingState.currentStepIndex + 1)

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Turn-by-Turn Maneuver Banner
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (routingState.hasArrived) RahnamaEmerald else MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (routingState.isRerouting) {
                    Text(
                        text = "مسیر جدید در حال محاسبه است...",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                    LinearProgressIndicator(
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (routingState.hasArrived) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(36.dp))
                        Column {
                            Text("به مقصد رسیدید!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                text = routingState.destination?.name ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                } else if (currentStep != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.18f),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Icon(
                                imageVector = maneuverIcon(currentStep.maneuverType, currentStep.maneuverModifier),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = GeoUtils.formatDistance(routingState.distanceToNextTurnMeters, distanceUnit),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentStep.instructionFa,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (currentStep.streetName.isNotBlank()) {
                                Text(
                                    text = currentStep.streetName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Next step preview + manual step inspector arrows
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (nextStep != null) {
                            Text(
                                text = "بعدی: ${nextStep.instructionFa}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.88f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                        Row {
                            IconButton(
                                onClick = onPrevStep,
                                enabled = routingState.currentStepIndex > 0,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "مرحله قبل", tint = Color.White)
                            }
                            IconButton(
                                onClick = onNextStep,
                                enabled = route != null && routingState.currentStepIndex + 1 < route.steps.size,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "مرحله بعد", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Bottom Navigation Telemetry Bar
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            shadowElevation = 10.dp,
            tonalElevation = 3.dp,
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${GeoUtils.formatDuration(routingState.remainingDurationSeconds)} • رسیدن ${GeoUtils.formatArrivalTimeTehran(routingState.remainingDurationSeconds)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = RahnamaEmerald
                    )
                    Text(
                        text = "${GeoUtils.formatDistance(routingState.remainingDistanceMeters, distanceUnit)} • ${GeoUtils.formatSpeedKmh(speedMps, distanceUnit)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        onClick = onToggleVoice,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("nav_voice_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (voiceEnabled) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                            contentDescription = stringResource(R.string.cd_voice_toggle),
                            tint = if (voiceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Button(
                        onClick = onStopNavigation,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("stop_navigation_button")
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("پایان")
                    }
                }
            }
        }
    }
}

private fun maneuverIcon(type: String, modifier: String?): ImageVector {
    return when (type) {
        "arrive" -> Icons.Rounded.Flag
        "roundabout", "rotary" -> Icons.Rounded.RoundaboutLeft
        else -> when (modifier) {
            "right", "sharp right", "slight right" -> Icons.Rounded.TurnRight
            "left", "sharp left", "slight left" -> Icons.Rounded.TurnLeft
            "uturn" -> Icons.Rounded.UTurnLeft
            else -> Icons.Rounded.Straight
        }
    }
}
