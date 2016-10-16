TinyTinyFeed
============

**You can get it from the Play Store [here](https://play.google.com/store/apps/details?id=org.poopeeland.tinytinyfeed)**

## Introduction


A Simple Widget to publish your [Tiny Tiny RSS Feeds](http://tt-rss.org) on your home screen

You need to have an account on a Tiny Tiny RSS installation (v1.12 at least) with the APIs enabled in order to use this widget.

The widget updates itself every 30 minutes and shows you the last 20 articles (you can change this value in the settings). 

Be aware that the widget update isn't in sync with the TTRss update.

Artworks from my wife [Namida](https://www.facebook.com/NamidaArt) (or [here](http://namida-art.com)) - Kuddos to her

If you want, buy me some [coffee](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=TPHRD64MV2B5U)

## Release notes

* V1.10
    * Status text now shows when the widget is updating
    * Fix the color of the refresh button

* V1.9
    * Add a nice refresh button, more user friendly than the "click on the status bar to refresh" thing.
    * Fix the null url issue
    * Fix the non-trimmed preference settings
    * Refactorisation and cleaning

* V1.8
    * Correct a bug that appear with version 1.15.3: the excerpt of the articles was empty. Now, you can choose the lenght of characters of the exceprt in the setting
    * Now you can choose background and text color, thanks to [AmbilWarna color chooser](https://code.google.com/p/android-color-picker/)

* V1.7 -- Users of older versions, you need to re-enter your setting. Sorry about that.
    * New Setup Activity
    * Better SSL integration
    * Various bug fixes and optimisation

* V1.6

    * Now accepts https connection

* V1.5

    * Add a black transparent background so the font is more visible now

* V1.4

    * Check the avaibility of the API in the setup Activity
    * Lock screen widget
    * Various bugfixes and optimisations

* V1.3:

    * Add an option to retrieve only unread feeds
    * You can now subscribe to rss feed by sharing links to the Tiny Tiny Feed (still in beta)
    * New Setup Screen
    
* V1.2:
    
    * Now there is two different layout for read and unread articles (Unread ones have a little [ * ] before the name)
    * Opening an article make it "as read" in TinyTinyRss and refresh the list

* V1.1:
 
    * Add basic HTTP auth support (thanks to Xeno for the report) (issue #1)
    * Allow to change the widget width (issue #2)
    * Change minimum version of android to 4.0 (issue #5)
    * Change the default language to english (issue #3)
    * Change the widget preview image (issue #4)

* V1.0:

    Initial release.
