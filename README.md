# face-ml [![](https://jitpack.io/v/shayantabatabaee/Face-ML.svg)](https://jitpack.io/#shayantabatabaee/Face-ML)

This repository is an android implementation for FaceDetection and FaceMesh modules that uses [MediaPipe](https://google.github.io/mediapipe/) tflite models.</br>
Since mediapipe android dependencies have big sizes, this implementation will help to reduce size of the final application.

## Installation
To use this library add <i>jitpack</i> maven url to repositories like this :

```bash
repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }        // <-------- Add this line
    }
```
</br>

For <b><i>detection</i></b> module add dependency like below: 
```bash
    implementation "com.github.shayantabatabaee.face-ml:core:${version}"
    implementation "com.github.shayantabatabaee.face-ml:detection:${version}"
```

For <b><i>landmark</i></b> module add dependency like below:
```bash
    implementation "com.github.shayantabatabaee.face-ml:core:${version}"
    implementation "com.github.shayantabatabaee.face-ml:detection:${version}"
    implementation "com.github.shayantabatabaee.face-ml:landmark:${version}"
```
This module Approximately add <b>7.5 mb</b> to project in total.

## Refrences
[MediaPipe](https://google.github.io/mediapipe/)</br>
[BlazeFace](https://arxiv.org/abs/1907.05047)

## License
The original mediapipe was distributed under the Apache 2.0 License, and so is this. I've tried to
keep the original copyright and author credits in place.






