# WeRecycle Android App
Source code for the WeRecycle android app for my Community Project 2019-2020.

## Purpose
Learn to sort your waste using an index of items and their respective bins and an scanner that uses Machine Learning to automatically determine the item's bin.

## Features
- Large index of items and their respective bins (ex. an apple goes into the compost bin)
- Machine Learning scanner that uses an image of an item to determine its bin

## Screenshots
![Image 1 of index](https://user-images.githubusercontent.com/29025984/113482566-30712680-946d-11eb-8921-50cb6085468f.png)
![Image 2 of scanner results](https://user-images.githubusercontent.com/29025984/113482579-45e65080-946d-11eb-820f-a9835ad25f8b.png)
![Image 3 of scanner](https://user-images.githubusercontent.com/29025984/113482586-526aa900-946d-11eb-9a08-982f5c3adf6f.png)
![Image 4 of index being used](https://user-images.githubusercontent.com/29025984/113482603-5f879800-946d-11eb-91de-3e0019a339af.png)

## Technologies
- General
  - Fragments are used to show each page
- Scanner
  - Uses MobileNetV1 for accurate and quick results on mobile devices
  - Uses TensorFlow Lite as the framework
  - AndroidX libraries are used for fetching the image from the camera and showing a viewfinder
  - Firebase ML is used for model distribution
- Index
  - Uses RecyclerView for simple lists
  - Uses Firestore for database

## App on Play Store
https://play.google.com/store/apps/details?id=com.aritrosaha.aritr.werecycleapp

## Copyright
© 2021 Aritro S, Mohil K, and Yusuf L. All rights reserved. 

Modification and redistribution of the source code is not allowed.  
