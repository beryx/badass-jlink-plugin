module org.beryx.modular.hello {
    requires slf4j.api;
    requires org.apache.logging.log4j.slf4j;
    requires org.apache.logging.log4j.core;
    requires org.apache.logging.log4j;

    exports org.beryx.modular.hello;
}
