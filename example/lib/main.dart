//  Copyright (c) 2018 Loup Inc.
//  Licensed under Apache License v2.0

import 'package:beacons/beacons.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';

import 'tab_monitoring.dart';
import 'tab_ranging.dart';

void main() => runApp(new MyApp());

class MyApp extends StatefulWidget {
  MyApp() {
    Beacons.loggingEnabled = true;

    int notifId = 0;

    Beacons.backgroundMonitoringEvents().listen((event) {
      FlutterLocalNotificationsPlugin flutterLocalNotificationsPlugin =
          new FlutterLocalNotificationsPlugin();
      AndroidInitializationSettings initializationSettingsAndroid =
          new AndroidInitializationSettings('app_icon');
      IOSInitializationSettings initializationSettingsIOS =
          new IOSInitializationSettings();
      InitializationSettings initializationSettings =
          new InitializationSettings(
              initializationSettingsAndroid, initializationSettingsIOS);
      flutterLocalNotificationsPlugin.initialize(initializationSettings);

      AndroidNotificationDetails androidPlatformChannelSpecifics =
          new AndroidNotificationDetails('your channel id', 'your channel name',
              'your channel description');
      IOSNotificationDetails iOSPlatformChannelSpecifics =
          new IOSNotificationDetails();
      NotificationDetails platformChannelSpecifics = new NotificationDetails(
          androidPlatformChannelSpecifics, iOSPlatformChannelSpecifics);
      flutterLocalNotificationsPlugin.show(
        ++notifId,
        event.type.toString(),
        event.state.toString(),
        platformChannelSpecifics,
      );
    });

    Beacons.configure(BeaconsSettings(
      android: BeaconsSettingsAndroid(
        logs: BeaconsSettingsAndroidLogs.info,
      ),
    ));
  }

  @override
  _MyAppState createState() => new _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  Widget build(BuildContext context) {
    return new MaterialApp(
      home: new CupertinoTabScaffold(
        tabBar: new CupertinoTabBar(
          items: <BottomNavigationBarItem>[
            new BottomNavigationBarItem(
              title: new Text('Track'),
              icon: new Icon(Icons.location_searching),
            ),
            new BottomNavigationBarItem(
              title: new Text('Monitoring'),
              icon: new Icon(Icons.settings_remote),
            ),
            new BottomNavigationBarItem(
              title: new Text('Settings'),
              icon: new Icon(Icons.settings_input_antenna),
            ),
          ],
        ),
        tabBuilder: (BuildContext context, int index) {
          return new CupertinoTabView(
            builder: (BuildContext context) {
              switch (index) {
                case 0:
                  return new RangingTab();
                case 1:
                  return new MonitoringTab();
                default:
                  return new Container(
                    child: new Center(
                      child: new Text('TBD'),
                    ),
                  );
              }
            },
          );
        },
      ),
    );
  }
}
