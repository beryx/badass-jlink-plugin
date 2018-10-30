module org.beryx.modular.hello {
    requires slf4j.api;
    //requires org.apache.logging.log4j.slf4j.impl;
    //requires org.apache.logging.log4j.core;
    //requires org.apache.logging.log4j;

    requires log4j.core;
    requires log4j.api;
    requires log4j.slf4j.impl;

    exports org.beryx.modular.hello;
}
