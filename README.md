# cordova-in-app-update

This pluging enabels [In app update](https://developer.android.com/guide/playcore/in-app-updates) For cordova.

## Setup

Adding on config.xml to prevent conflict when many plugins try to edit same manifest's attribute

```
<custom-preference name="android-manifest/application/activity[@android:name='MainActivity']/@android:theme" value="@style/Theme.AppCompat.NoActionBar" />
```

## Supports

- Flexible update
- Immidiate update
- Stalneess days For both type of updates

## Configs

**Note** :
Stalness days: How many times the user has already been asked to update.
_Setting it to 0 will trigger update flow on the 1st attempt._

- **flexibleUpdateStalenessDays** : Provide stalness days for flexible update
- **immediateUpdateStalenessDays** : Provide stalness days for immidiate update

## Examples

```javascript
window.plugins.updatePlugin.update(
  function (available) {
    console.log('The new app version available', available);
  },
  function (e) {
    console.error('Update plugin failed', e);
  },
  {
    ANDROID: {
      type: 'MIXED',
      flexibleUpdateStalenessDays: 0,
      immediateUpdateStalenessDays: 0,
    },
    IOS: {
      type: 'MIXED',
      flexibleUpdateStalenessDays: 0,
      immediateUpdateStalenessDays: 0,
      alertTitle: 'New Version',
      alertMessage:
        'version __version__ of __appName__ is available on the AppStore.',
      alertCancelButtonTitle: 'Update',
      alertUpdateButtonTitle: 'Not Now',
    },
  }
);
```
