/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
/**
 * @author Mario Pastorelli (mario.pastorelli@teralitycs.ch)
 */
package pureconfig.example

import java.nio.file.{Path, Paths}

import pureconfig.ConfigConvert.fromString

/*
This is an example of configuration for our directory watcher. The configuration needs:
- the path that is the directory to watch
- a filter that will be used to decide if a path should be notified or not
- the email configuration used to send emails

The root namespace will be "dirwatch". For instance, valid property file for this
configuration will contain:

dirwatch.path="/path/to/observe"
dirwatch.filter="*"
dirwatch.email.host=host_of_email_service
dirwatch.email.port=port_of_email_service
dirwatch.email.message="Dirwatch new path found report"
dirwatch.email.recipients="recipient1,recipient2"
dirwatch.email.sender="sender"
*/
package object conf {
  // path doesn't have a StringConvert instance, we are going to create it here
  implicit val deriveStringConvertForPath = fromString[Path](Paths.get(_))
}
