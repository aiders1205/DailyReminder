# Daily Reminder

A simple Android app for setting daily reminders and receiving them as notifications.
Case study project for ELECTIVE1 by Group LIBRAS.

## Features

- Two screens: a list of reminders, and a screen to add or edit a reminder
- Each reminder has a title, a description and a notification time
- Daily notifications that show the title and description
- Edit and delete reminders from the list
- Mark reminders as completed
- Repeat on specific days of the week (for example, Monday and Wednesday)
- Custom notification sound for each reminder
- Snooze button on the notification (5, 10, 15 or 30 minutes)

## Privacy

- A Data Privacy Notice based on the Data Privacy Act of 2012 (RA 10173), plus short Terms of Use, are shown on first launch. The user must tick both boxes before the app can be used.
- Users can view the notice again or withdraw consent from the menu. Withdrawing deletes all reminders, alarms, the database and the encryption key.
- Reminder titles and descriptions are encrypted (AES-256-GCM, key kept in the Android Keystore).
- Data stays on the phone: no internet permission, no account, cloud backups disabled.
- The phone's screen lock is required before reminders are shown.

## How to run

1. Open this folder in Android Studio.
2. Let Gradle sync.
3. Press **Run** to install the app on an emulator or a phone (Android 7.0 or newer).

Unit tests:

```
gradlew test
```

## Built with

Java, XML layouts, SQLite, AlarmManager and Material Components.

## Group LIBRAS

- Cobby N. Baldonado
- Edrian C. Necesario
- Adrianne Quinn Emmanuel C. Rosalan
- Buzz Aldrizh G. Saavedra
- Mhel Klarenz B. Urdaneta
