package org.keedio.datagenerator.config

import org.springframework.context.annotation.{Configuration, Import, Profile}

@Configuration
@Profile(Array("app"))
class DataGeneratorConfigApp {

}