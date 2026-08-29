# Pink Pixel Clock — Android Home-Screen Widget

This project turns the supplied pink pixel-art clock into a real Android analog clock widget.

## What it does
- Uses the supplied artwork as the clock face.
- Removes the baked-in sample hands.
- Draws hour, minute, and second hands from the phone's current local time.
- Updates on the minute boundary.
- Reschedules after reboot/timezone/time changes.
- The widget can be resized.

## Build
Open this folder in Android Studio (Ladybug or newer recommended) and let Gradle sync.

Then:
1. Build > Make Project.
2. Install the app on your Android phone.
3. Long-press the home screen.
4. Widgets > Pink Pixel Clock.
5. Add and resize it as desired.

On Android 12+, the app can use exact alarms when the user/device allows them. If exact alarms are unavailable, it falls back to a repeating alarm, which Android may batch for battery life.

The widget is intentionally minute-accurate rather than updating every second: Android home-screen widgets are heavily restricted from second-by-second background updates.
