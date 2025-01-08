# TypeAware

The TypeAware application is built using Kotlin programming language and targets Android
platforms. The architecture of this app follows Android's MVVM (Model-View-ViewModel) pattern.

The goal of the application is to act as a means for collecting smartphone
usage and sleep data from users to provide large amounts of data that can be studied and
utilised when building sleep recommender platforms. To reach this goal, the required
features of this application include collecting smartphone usage metrics relevant to
typing such as errors made by the user or their typing speed and the sleep data collected
with a Fitbit device. 
In addition, for this study, an additional feature of conducting daily
questionnaires to gain insights about the users’ perceptions of their data is integrated
into the application. 

Fitbit data is collected by Fitbit devices with sleep-tracking capabilities. TypeAware retrieves the sleep data by first establishing an OAuth2.0 protocol with
Fitbit’s API. For this application, the application type is set as client and Authorization
Code Grant Flow with Proof Key for Code Exchange (PKCE) is used for establishing
the connection.

This project or its contents is not allowed to be used for commercial purposes. 

UI explained:

## Onboarding

When first downloading the app and using it for the first time, the user is guided
through onboarding. The three main goals of onboarding are asking for consent (screen not included in this documentation),
receiving user information and introducing the application.

<img src="https://github.com/user-attachments/assets/b7c3f6b7-ab97-46c4-b43f-75e20c77b652" width="180">
<img src="https://github.com/user-attachments/assets/f635f5a3-bcd6-4630-8635-42882a60d78b" width="180">
<img src="https://github.com/user-attachments/assets/61b2ede9-748f-47d6-ba5d-a192077a14dc" width="180">
<img src="https://github.com/user-attachments/assets/7ed41029-78e2-4f24-8a03-76347fefe783" width="180">
<img src="https://github.com/user-attachments/assets/9a447646-b572-4518-9d01-4ad47e8626a3" width="180">


## Home

Home screen is the landing page after onboarding and when first opening the application. It provides a quick way to check that everything is working properly and gives an overview to the user of their daily data.
This view handles many scenarios, such as user not logged into Fitbit, data not found or permissions missing (visible in any view when opening the app). This alerts the user to take necessary actions or contact support if there are any issues.


<img src="https://github.com/user-attachments/assets/de6497e4-7554-4413-b3c2-93640ca995f4" width="160">
<img src="https://github.com/user-attachments/assets/7d6c8b65-22cf-44cd-990f-cd0c7d0b1de1" width="160">
<img src="https://github.com/user-attachments/assets/c7c44e8d-cb16-4afa-9ce8-f438b248c72e" width="160">
<img src="https://github.com/user-attachments/assets/4924c32a-ee71-4fb8-929f-2e02dd8607bc" width="160">
<img src="https://github.com/user-attachments/assets/e7dd4f5c-3e0e-4c28-94b0-f7a20f12e1ea" width="160">

## Charts

The charts screen offers a more accurate display of all the data collected
by the application. This scroll view contains charts on daily keyboard usage as well as weekly sleep cycles.
The charts are implemented using MPAndroidChart’s library.

<img src="https://github.com/user-attachments/assets/c13ced82-ffef-4e4b-9559-a046d64bc5af" width="160">
<img src="https://github.com/user-attachments/assets/1095cb15-05e8-46ad-b591-a8b699218f75" width="160">


## Settings

The settings screen helps the users check that every setting
for the application is optimal. For some users, it can be confusing that they first
need to enable the accessibility settings and then turn off battery optimisation, so the
settings screen is designed to indicate the right settings by showing a green marker for
a correctly set setting and a red marker for a setting that needs to be set differently.
Text is also added to prompt the user to take action if they see a red marker.

<img src="https://github.com/user-attachments/assets/d55ca761-913f-4a1c-9f7e-92d94331b4c7" width="180">


## Questionnaires

Due to the nature of the study, the participants were
instructed to complete ten daily questionnaires and also an end questionnaire.
The mandatory questionnaires were divided into three parts: week one, week
two and an end questionnaire, where week one and two last five days and the end
questionnaire can be answered after five questionnaires for week one and five for week
two are completed. The nature of the questions varies, some are open-ended, some request numerical values and some are answered on the Likert scale. Week one questionnaires pop up automatically if not answered for that day but during week two the user can choose when they prefer to answer the daily questionnaire.
The questionnaires were implemented using Android's DialogFragment. 

<img src="https://github.com/user-attachments/assets/07867404-327f-49d1-a6e5-66708939e88a" width="160">
<img src="https://github.com/user-attachments/assets/85c1b544-06bb-43bc-a2a0-3ca23cd2f175" width="160">
<img src="https://github.com/user-attachments/assets/1bfc1c94-ca62-489e-b2ae-11798cf3d4dc" width="160">
<img src="https://github.com/user-attachments/assets/db0dc935-8fc6-49a7-be2a-21ce667234ac" width="160">
<img src="https://github.com/user-attachments/assets/7c37ae14-d47a-43be-9dd0-30ea9b89a5d4" width="160">

The app also sends notifications reminding the user to answer their daily questionnaires. 
The notifications are implemented using Android’s BroadcastReceiver to receive an
intent broadcast set by AlarmManager to occur daily at 6 pm if the questionnaire
is not answered. After receiving the intent, the notification will be posted by using
NotificationManager to post on a notification channel created by the application.




