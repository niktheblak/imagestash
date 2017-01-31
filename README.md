image-resizer
=============

An image thumbnail generation system that is built on Clojure and Compojure.

- Provides an image broker actor that fetches the requested image, resizes it, caches it to a Facebook Haystack-style cache and returns the cached version on subsequent requests
- Scalable
- Capable of handling large amounts of image traffic

## Installation

Clone the repository

    $ git clone https://github.com/niktheblak/imagestash.git

## Usage

Start the service with Leiningen:

    $ lein ring server-headless

After this the service exposes a web API at http://localhost:8080/resize. The API takes the following parameters:

| Name   | Type    | Required | Description                                   |
|--------|-------- |----------|-----------------------------------------------|
| source | String  | x        | URL of the source image                       |
| size   | Integer | x        | Desired size of the image                     |
| format | String  |          | Desired image format (`jpeg`, `png` or `gif`) |

For example:

    http://localhost:8080/resize?source=https://channelcloud-a.akamaihd.net/mobilevideopanel_2c9314d9aa5f4c4a_364x618_29ada748cb59b5dd.JPEG&size=800

## License

Copyright 2016 Niko Korhonen

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
