##### Build status: [![Build Status](https://travis-ci.org/BizzyIzDizzy/configuration-server.svg?branch=master)](https://travis-ci.org/BizzyIzDizzy/configuration-server)

# Configuration server

Lightweight centralized configuration server.

__BEWARE THIS IS A SPECIFICATION IN PROGRESS AND COULD BE USELESS TO YOU__

Eventually this will be a real _README_ but for now it's serving as my technical specification and though flow target surface.

## Purpose

Enables you to easily manage sets of configurations that can be used by other services in your application stack. There are some other configuration servers out there (like Spring Config or maybe even Zookeeper) but I didn't find them easy to use/configure. Something that enables you to easily distribute configurations should on itself be easily configurable IMHO.

Also I just wanted some fun project for myself while learning kotlin...

## Challenges and solutions

For a team that is developing an application configuration management is usually a pain (I know from experience). I always wanted to have a centralized configuration server where each developer could have their own sets of configurations. 

Here are the challenges:
1. Each developer can be working on multiple features on multiple branches - switching between configurations should be __seamless and as easy as possible__
2. No one wants to manage configuration if it's not really needed - __configuration should be inheritable__ (like node-config files that all inherit from default)
3. You should always be able to connect configuration and the code at the exact commit - __configurations should be versioned just like the code is__

### First problem
To solve the first problem I imagined that configuration URL should uniquelly determine the configuration using:
* Configuration path on the server
* Configuration version

For example URL `http://config-server-ip/projects/new-project/developer/jack/feature/new-feature?v=3`would imply that developer _Jack_ created a new configuration for feature _New feature_ for project _New project_ and he needs 3rd version of the configuration.

So the only thing needed now for an application to function should be this configuration server url.

__But now we have another problem.__
This configuration perhaps inherits this configuration:
`http://config-server-ip/projects/new-project/master?v=113`

And developer only wants to change a few configuration properties so his configuration should be much smaller and easily navigatable (which is good and the reason why he is usign configuration server with inheritable configurations anyways).

The problem here is that when he completes _New feature_ and wants to merge it to master the configuration URL should not be merged but two things should perhaps be done:
1. If needed merge _new feature_ configuration to _master_ and then delete _new feature_ configuration
2. On master branch keep the existing configuration URL and increment configuration version

Of course thats a lot of work to do everytime and it's not ideal.

Now the solution for this is to integration configuration server with GIT version control and to make developers authenticate themselves uniquelly.

Then you would only need this configuration URL:
`http://config-server-ip/projects/new-project`

__To decouple configuration server from version control this should be done by a client side connector library. That way there is no need to make sure configuration server knows anything about version control that is being used on the code.__

Thats great - but of course now we have __another problem.__
_How to distribute configuration when deploying to different envs?_

The solution - materialize configuration on application deploy.
We __can__ do that because configuration server knows which configuration version to take because it's connected to version control. It also makes application deployable without configuration server.

Since the configuration of a deployable application is also determined by target environment there should be a way to automatically choose a _flawor_ of configuration that is being deployed. For example: _production_, _rc staging_, _dev staging_ etc.

Flawors would determine what properties should change upon materializing configuration and how. That way for example your `logging.level=TRACE` can change to `logging.level=WARN` when deploying to production.

__This could be done either through configuration server console or through configuration server REST API call.__