# Insert Affiliate SDK

A brief description of your SDK, its purpose, and what it offers.

## Table of Contents

1. [Installation](#installation)
2. [Getting Started](#getting-started)
3. [Usage](#usage)

## Features

- List the key features of your SDK.
- Highlight any unique aspects or functionalities.

## Installation

Instructions on how to install the SDK. Include different methods if applicable (e.g., Maven, Gradle, manual download).

Step 1. Add the JitPack repository to your build file

Add it in your root build.gradle at the end of repositories:

```bash
dependencyResolutionManagement {
		repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
		repositories {
			mavenCentral()
			maven { url 'https://jitpack.io' }
		}
	}
 ```

Step 2. Add the dependency

```bash
dependencies {
	        implementation 'com.github.Insert-Affiliate:InsertAffiliateAndroidSDK:v1.0.0'
}
```

## Usage

```bash
public class MainActivity extends AppCompatActivity {
    SharedPreferences sharedPreferences;
    InsertAffiliateManager insertAffiliateManager;
    private ActivityMainBinding binding;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //Copy This code to your main activity
        insertAffiliateManager = new InsertAffiliateManager(MainActivity.this);
        insertAffiliateManager.init(MainActivity.this);

}
```



