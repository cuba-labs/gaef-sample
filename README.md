# Sample CUBA Project to deploy to Google App Engine

Here are steps to deploy and run the CUBA application in Google App Engine Flexible.

## Google Cloud SDK Installation

Install the Cloud SDK. See [the documentation](https://cloud.google.com/sdk/docs/).

When the installation is finished, create a new project and name it cuba-sample. If you skip this step, then you’ll need to create the project in the web console.

Install the app-engine-java component: gcloud components install app-engine-java.

## Create Google App Engine Project

If you skipped project creation, use the [GCP Console](https://console.cloud.google.com) to do it.
Open the console and click on the sample project name on the top of the screen. Create new project. 

![Create Application, Step 1](/img/app_create_1.png)

Give project the name _cuba-sample_. Create an application using Java language

![Create Application, Step 2](/img/app_create_2.png)

Specify a region. The default on is _US-Central_ zone. You can choose another if you'd like to.

![Create Application, Step 3](/img/app_create_3.png)

Please note project ID – in this case it is ```cuba-sample-278518```

## Create Google Cloud Database

Choose SQL menu and click on it.

![Create Database, Step 1](/img/db_create_1.png)

Create cloud SQL instance

![Create Database, Step 2](/img/db_create_2.png)

Select PostgreSQL

![Create Database, Step 3](/img/db_create_3.png)

Assign the instance name. In this case, it is ```cuba-sample-db```. Specify the password for the postgres user 
(```postgres``` in this case) and select the region. Here we choose the same region as for the application: _US-Central_

![Create Database, Step 4](/img/db_create_4.png)
 
![Create Database, Step 5](/img/db_create_5.png)

Dive into the DB instance settings 

![Create Database, Step 6](/img/db_create_6.png)

and create a new database. For this application, its name is ```gaef```.

![Create Database, Step 7](/img/db_create_7.png)

![Create Database, Step 8](/img/db_create_8.png)

As stated in this [documentation section](https://cloud.google.com/sql/docs/postgres/admin-api/) you’ll need to enable the Cloud SQL Admin API. Follow the “Enable API” link 
from this section and select a ```cuba-sample``` project in the appeared dialog. Click “Continue”.

![Create Database, Step 9](/img/db_create_9.png)

## Specify Database for CUBA Application

Open the CUBA-> Main Data Store Settings in the CUBA Studio and change the database type to PostgreSQL, datasource should be defined in application.

![Connect App to DB, Step 1](/img/cuba_connect_1.png)

Open the CUBA -> Deployment -> Edit UberJar Settings  menu and specify Uber JAR deployment options as shown on the picture:
* Check the ‘Build Uber JAR’ checkbox
* Generate the Logback configuration file

![Connect App to DB, Step 2](/img/cuba_connect_2.png)

We will use runtime profiles to change database connection properties for Google Application Engine deploy. Create file gae-app.properties in CORE module.

![Connect App to DB, Step 3](/img/cuba_connect_3.png)

Add GAE-specific PostgreSQL connection. The database URL should conform the format described in the [manual](https://cloud.google.com/appengine/docs/flexible/java/using-cloud-sql-postgres), i.e.:

```
jdbc:postgresql://google/${database}?useSSL=false& cloudSqlInstance=${INSTANCE_CONNECTION_NAME}& socketFactory=com.google.cloud.sql.postgres.SocketFactory&amp;user=${user}& password=${password}
```
Instance connection name can be copied from the database properties in the web console.

![Connect App to DB, Step 4](/img/cuba_connect_4.png)

For this application, the profile-specific properties will look like this:

```properties
cuba.dataSource.username=postgres
cuba.dataSource.password=postgres
cuba.dataSource.dbName=gaef
cuba.dataSource.host=google
cuba.dataSource.connectionParams=?useSSL=false&cloudSqlInstance=cuba-sample-278518:us-central1:cuba-sample-db&socketFactory=com.google.cloud.sql.postgres.SocketFactory
```
 To connect deployed application to the cloud database, you need to enable VPC connector according to [documentation](https://cloud.google.com/vpc/docs/configure-serverless-vpc-access).
 
![Connect App to DB, Step 5](/img/cuba_connect_5.png)

## Prepare the Application for Deploy to Google App Engine
 
Create the app.yaml file in the appengine directory in the root of the project. Please note that we use project ID and conector name that we defined earlier. Entrypoint is important,
in this line we specify the following:
* ```-Dapp.home=/tmp/app_home``` - specifies temporary folder where CUBA stores its settings
* ```-Dspring.profiles.active=gae``` - enables settings values specified in ```gae-app.properties``` file. Double check that spring profile name and file name suffix are the same.

```yaml
runtime: java11
manual_scaling:
  instances: 1
entrypoint: "java -Dapp.home=/tmp/app_home -Dspring.profiles.active=gae -jar app.jar"
vpc_access_connector:
  name: "projects/cuba-sample-278518/locations/us-central1/connectors/gaef-connector"
```

You can find more about ```app.yaml``` syntax in the [documentation](https://cloud.google.com/appengine/docs/standard/java11/config/appref).

## Update Application Build Script

The next step - configure deployment using the [Gradle plugin](https://github.com/GoogleCloudPlatform/app-gradle-plugin).
 
Add a dependency - appengine-gradle-plugin in the build.gradle:

```groovy
dependencies {
    classpath "com.haulmont.gradle:cuba-plugin:$cubaVersion"
    classpath 'com.google.cloud.tools:appengine-gradle-plugin:2.2.0'
}
```
In the end of the ```build.gradle``` file add the required gradle tasks:

```groovy
apply plugin: 'com.google.cloud.tools.appengine'

appengine {
    stage {
        artifact = "$buildDir/distributions/uberJar/app.jar"
        appEngineDirectory = 'appengine'    // a directory with app.yaml
        stagingDirectory = "$buildDir/staged-app"
    }

    deploy {
        projectId = 'cuba-sample-278518'     // specify a project id if the current project is not the default one
        stopPreviousVersion = true           // default - stop the current version
        promote = true                       // default - & make this the current version
        version = 'GCLOUD_CONFIG'
    }
}

appengineStage.dependsOn(buildUberJar)

// a dummy task. It is required for appengineStage task of the google plugin
task assemble {
    doLast {}
}
```
In the dependencies section of the coreModule add a dependency to the postgres-socket-factory:
```groovy
dependencies {
    compile(globalModule)
    compileOnly(servletApi)
    jdbc(postgres)
    jdbc('com.google.cloud.sql:postgres-socket-factory:1.0.16') {
        exclude group: 'com.google.guava', module: 'guava'
    }
    testRuntime(postgres)
}
```

Please note the exclusion of the guava library in the dependencies. If you don’t do it, you can get a problem because of CUBA’s and SocketFactory libraries clash. 
```
java.lang.NoSuchMethodError: 'java.util.stream.Collector com.google.common.collect.ImmutableList.toImmutableList()'
```
Change the PostgreSQL JDBC driver version:
```groovy
def postgres = 'org.postgresql:postgresql:42.2.12'
```
Then run the ```appengineDeploy``` gradle task and the project should be deployed:
```
./gradlew appengineDeploy
```

After the deployment is completed, open the application URL in the browser and add the /app to the end of the URL similar to this:
[https://cuba-sample-278518.uc.r.appspot.com/app](https://cuba-sample-278518.uc.r.appspot.com/app)

An important note here. You will not be able to save files using the standard FileStorage. If your application uses it, then you 'll have to do something with files persistence. 
There is a sample implementation of such storage can be foung on [GitHub](https://gist.github.com/telenieko/2250095c12ccbf44dd6c14f9ae4c98a6).

 
