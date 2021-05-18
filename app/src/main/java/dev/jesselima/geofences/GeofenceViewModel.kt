package dev.jesselima.geofences

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

/*
 * This class contains the state of the game.  The two important pieces of state are the index
 * of the geofence, which is the geofence that the game thinks is active, and the state of the
 * hint being shown.  If the hint matches the geofence, then the Activity won't update the geofence
 * as it cycles through various activity states.
 *
 * These states are stored in SavedState, which matches the Android lifecycle.  Destroying the
 * associated Activity with the back action will delete all state and reset the game, while
 * the Home action will cause the state to be saved, even if the game is terminated by Android in
 * the background.
 */

private const val KEY_HINT_INDEX_KEY = "hintIndex"
private const val KEY_GEOFENCE_INDEX_KEY = "geofenceIndex"

class GeofenceViewModel(state: SavedStateHandle) : ViewModel() {

}

