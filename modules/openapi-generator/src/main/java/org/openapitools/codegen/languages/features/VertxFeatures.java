package org.openapitools.codegen.languages.features;

public interface VertxFeatures extends BeanValidationFeatures, PerformBeanValidationFeatures {

  String USE_DATAOBJECT_OPTION = "useDataObject";

  String USE_FUTURE_OPTION = "useFuture";

  String VERTX_VERSION = "vertxVersion";

  String RX_INTERFACE_OPTION = "rxInterface";

  void setUseDataObject(boolean useDataObject);

  void setUseFuture(boolean useFuture);

  void setVertxVersion(String vertxVersion);

  void setRxInterface(boolean rxInterface);

}
