import org.jspecify.annotations.NullMarked;

@NullMarked
module org.beryx.modular.annotatedmodule {
    requires org.jspecify;
    opens org.beryx.modular.annotatedmodule;
}
