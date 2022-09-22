Cloudinary React SDK
=========================
## About
The Cloudinary React SDK allows you to quickly and easily integrate your application with Cloudinary.
Effortlessly optimize and transform your cloud's assets.

#### Note
This Readme provides basic installation and usage information.
For the complete documentation, see the [React SDK Guide](https://cloudinary.com/documentation/react_integration).

## Table of Contents
- [Key Features](#key-features)
- [Version Support](#Version-Support)
- [Installation](#installation)
- [Usage](#usage)
    - [Setup](#Setup)
    - [Transform and Optimize Assets](#Transform-and-Optimize-Assets)
    - [Generate Image and HTML Tags](#Generate-Image-and-Video-HTML-Tags)
    - [Plugins](#Advanced-Plugin-Features)

## Key Features
- [Transform](https://cloudinary.com/documentation/react_video_manipulation#video_transformation_examples) and
 [optimize](https://cloudinary.com/documentation/react_image_manipulation#image_optimizations) assets.
- Generate [image](https://cloudinary.com/documentation/react_image_manipulation#deliver_and_transform_images) and
 [video](https://cloudinary.com/documentation/react_video_manipulation#video_element) tags.

## Version Support
| SDK Version   | React 16   | React 17 | React 18 |
|---------------|------------|----------|----------|
| 1.0.0 & up    | V          | V        | V        |

## Installation
### Install using your favorite package manager (yarn, npm)
```bash
npm i @cloudinary/url-gen @cloudinary/react --save

```
Or
```bash
yarn add @cloudinary/url-gen @cloudinary/react --save
```

## Usage
### Setup
```javascript
import React from 'react';
import { AdvancedImage } from '@cloudinary/react'
import {CloudinaryImage} from '@cloudinary/url-gen';
```

### Transform and Optimize Assets
- [See full documentation](https://cloudinary.com/documentation/react_image_manipulation)

```tsx
import React, { Component } from 'react'

import { AdvancedImage } from '@cloudinary/react'
import {CloudinaryImage} from '@cloudinary/url-gen';

const myCld = new Cloudinary({ cloudName: 'demo'});
let img = myCld().image('sample');

const App = () => {
  return <AdvancedImage cldImg={img}/>
};
```
    ```
### Generate Image and Video HTML Tags
    - Use <AdvancedImage> to generate image tags
    - Use <AdvancedVideo> to generate video tags

### Advanced Plugin Features
- [See full documentation](https://cloudinary.com/documentation/react_integration#plugins)
<br/><br/>
We recommend the following order when using our plugins to achieve the best results:
<br/><br/>

```tsx
import { CloudinaryImage } from "@cloudinary/url-gen";
import {
  lazyload,
  responsive,
  accessibility,
  placeholder
} from "@cloudinary/react";

cloudinaryImage = new CloudinaryImage("sample", { cloudName: "demo" });

const App = () => {
  return <AdvancedImage cldImg={img} plugins = {[lazyload(),responsive(), accessibility(), placeholder()]};/>
};
```

You can omit any plugin, but the order from above should remain.

### File upload
This SDK does not provide file upload functionality, however there are [several methods of uploading from the client side](https://cloudinary.com/documentation/react_image_and_video_upload).

## Contributions
- Ensure tests run locally (```npm run test```)
- Open a PR and ensure Travis tests pass

## Get Help
If you run into an issue or have a question, you can either:
- [Open a Github issue](https://github.com/cloudinary/frontend-frameworks/issues)  (for issues related to the SDK)
- [Open a support ticket](https://cloudinary.com/contact) (for issues related to your account)

## About Cloudinary
Cloudinary is a powerful media API for websites and mobile apps alike, Cloudinary enables developers to efficiently manage, transform, optimize, and deliver images and videos through multiple CDNs. Ultimately, viewers enjoy responsive and personalized visual-media experiences—irrespective of the viewing device.


## Additional Resources
- [Cloudinary Transformation and REST API References](https://cloudinary.com/documentation/cloudinary_references): Comprehensive references, including syntax and examples for all SDKs.
- [MediaJams.dev](https://mediajams.dev/): Bite-size use-case tutorials written by and for Cloudinary Developers
- [DevJams](https://www.youtube.com/playlist?list=PL8dVGjLA2oMr09amgERARsZyrOz_sPvqw): Cloudinary developer podcasts on YouTube.
- [Cloudinary Academy](https://training.cloudinary.com/): Free self-paced courses, instructor-led virtual courses, and on-site courses.
- [Code Explorers and Feature Demos](https://cloudinary.com/documentation/code_explorers_demos_index): A one-stop shop for all code explorers, Postman collections, and feature demos found in the docs.
- [Cloudinary Roadmap](https://cloudinary.com/roadmap): Your chance to follow, vote, or suggest what Cloudinary should develop next.
- [Cloudinary Facebook Community](https://www.facebook.com/groups/CloudinaryCommunity): Learn from and offer help to other Cloudinary developers.
- [Cloudinary Account Registration](https://cloudinary.com/users/register/free): Free Cloudinary account registration.
- [Cloudinary Website](https://cloudinary.com): Learn about Cloudinary's products, partners, customers, pricing, and more.


## Licence
Released under the MIT license.
