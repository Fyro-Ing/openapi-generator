/*
 * Copyright 2018 OpenAPI-Generator Contributors (https://openapi-generator.tech)
 * Copyright 2018 SmartBear Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openapitools.codegen.languages;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.servers.Server;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.CodegenType;
import org.openapitools.codegen.SupportingFile;
import org.openapitools.codegen.languages.features.BeanValidationFeatures;
import org.openapitools.codegen.languages.features.VertxFeatures;
import org.openapitools.codegen.meta.GeneratorMetadata;
import org.openapitools.codegen.meta.Stability;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.OperationMap;
import org.openapitools.codegen.model.OperationsMap;
import org.openapitools.codegen.utils.URLPathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaVertXServerCodegen extends AbstractJavaCodegen implements VertxFeatures {
    private final Logger LOGGER = LoggerFactory.getLogger(JavaVertXServerCodegen.class);

    protected String resourceFolder = "src/main/resources";
    protected String invokerPackage = "org.openapitools.vertx.server";
    protected String apiVersion = "1.0.0-SNAPSHOT";

    @Getter @Setter
    protected boolean performBeanValidation = false;
    @Getter @Setter
    protected boolean useDataObject = false;
    @Getter @Setter
    protected boolean useFuture = true;
    @Getter @Setter
    protected String vertxVersion = "4.5.15";
    @Getter @Setter
    protected boolean notNullJacksonAnnotation = false;
    @Getter @Setter
    protected boolean rxInterface = false;

    private static final String VERTX_SWAGGER_ROUTER_VERSION_OPTION = "vertxSwaggerRouterVersion";
    private static final String VERTX_VERSION_5 = "vertxV5";

    /**
     * A Java Vert.X generator. It uses java8 date API. It can be configured with 2 CLI options :
     * <p>
     * rxInterface : type Boolean if true, API interfaces are generated with RX and methods return
     * Single and Comparable. default : false
     * <p>
     * vertxSwaggerRouterVersion : type String Specify the version of the swagger router library
     */
    public JavaVertXServerCodegen() {
        super();

        generatorMetadata = GeneratorMetadata.newBuilder(generatorMetadata)
                .stability(Stability.BETA)
                .build();

        // set the output folder here
        outputFolder = "generated-code" + File.separator + "java-vertx";

        modelTemplateFiles.clear();
        modelTemplateFiles.put("model.mustache", ".java");

        apiTemplateFiles.clear();
        apiTemplateFiles.put("api.mustache", ".java");
        apiTemplateFiles.put("apiVerticle.mustache", "Verticle.java");
       // apiTemplateFiles.put("apiException.mustache", "Exception.java");

        embeddedTemplateDir = templateDir = "JavaVertXServer";

        apiPackage = invokerPackage + ".verticle";
        modelPackage = invokerPackage + ".model";
        artifactId = "openapi-java-vertx-server";
        artifactVersion = apiVersion;
        this.setDateLibrary("java8");
        useBeanValidation = false;

        // clioOptions default redefinition need to be updated
        updateOption(CodegenConstants.ARTIFACT_ID, this.getArtifactId());
        updateOption(CodegenConstants.ARTIFACT_VERSION, this.getArtifactVersion());
        updateOption(CodegenConstants.API_PACKAGE, apiPackage);
        updateOption(CodegenConstants.MODEL_PACKAGE, modelPackage);
        updateOption(CodegenConstants.INVOKER_PACKAGE, invokerPackage);
        updateOption(DATE_LIBRARY, this.getDateLibrary());
        updateOption(BeanValidationFeatures.USE_BEANVALIDATION, String.valueOf(useBeanValidation));

        cliOptions
          .add(CliOption.newBoolean(USE_BEANVALIDATION, "Use BeanValidation API annotations", useBeanValidation));
        cliOptions.add(CliOption.newBoolean(PERFORM_BEANVALIDATION,
          "Use Bean Validation Impl. to perform BeanValidation", performBeanValidation));

        cliOptions.add(CliOption.newBoolean(RX_INTERFACE_OPTION,
                "When specified, API interfaces are generated with RX "
                        + "and methods return Single<> and Comparable."));
        cliOptions.add(CliOption.newString(VERTX_SWAGGER_ROUTER_VERSION_OPTION,
                "Specify the version of the swagger router library"));

        cliOptions.add(CliOption.newBoolean(USE_DATAOBJECT_OPTION,
          "When specified, models objects are generated with @DataObject", useDataObject));
        cliOptions.add(CliOption.newBoolean(USE_FUTURE_OPTION,
          "When specified, describe service as future or not. Default as false. Unused with Vert.X V5",
          useFuture));
        cliOptions.add(CliOption.newString(VERTX_VERSION, "Vert.x version", vertxVersion));
    }

    @Override
    public CodegenType getTag() {
        return CodegenType.SERVER;
    }

    @Override
    public String getName() {
        return "java-vertx";
    }

    @Override
    public String getHelp() {
        return "Generates a Java Vert.x Server.";
    }

    @Override
    public void processOpts() {
        super.processOpts();

        convertPropertyToBooleanAndWriteBack(PERFORM_BEANVALIDATION, this::setPerformBeanValidation);

        convertPropertyToBooleanAndWriteBack(USE_DATAOBJECT_OPTION, this::setUseDataObject);
        convertPropertyToBooleanAndWriteBack(USE_FUTURE_OPTION, this::setUseFuture);
        convertPropertyToBooleanAndWriteBack(RX_INTERFACE_OPTION, this::setRxInterface);
        if (additionalProperties.containsKey(VERTX_VERSION)) {
            setVertxVersion(additionalProperties.get(VERTX_VERSION).toString());
        }
        // additionalProperties.put(VERTX_VERSION, this.vertxVersion);
        if (vertxVersion.startsWith("5")) {
            additionalProperties.put(VERTX_VERSION_5, true);
            additionalProperties.put(USE_FUTURE_OPTION, true);
        }

        apiTestTemplateFiles.clear();

        importMapping.remove("JsonCreator");
        importMapping.remove("com.fasterxml.jackson.annotation.JsonProperty");
        importMapping.put("JsonInclude", "com.fasterxml.jackson.annotation.JsonInclude");
        importMapping.put("JsonProperty", "com.fasterxml.jackson.annotation.JsonProperty");
        importMapping.put("JsonValue", "com.fasterxml.jackson.annotation.JsonValue");
        importMapping.put("FileUpload", "io.vertx.ext.web.FileUpload");
        importMapping.put("JsonObject", "io.vertx.core.json.JsonObject");
        importMapping.put("JsonArray", "io.vertx.core.json.JsonArray");
        importMapping.put("MainApiException", invokerPackage + ".MainApiException");

        modelDocTemplateFiles.clear();
        apiDocTemplateFiles.clear();

        String sourcePackageFolder = sourceFolder + File.separator + invokerPackage.replace(".", File.separator);
        supportingFiles.clear();
        supportingFiles.add(new SupportingFile("openapi.mustache", resourceFolder, "openapi.json"));
        supportingFiles.add(new SupportingFile("MainApiVerticle.mustache", sourcePackageFolder, "MainApiVerticle.java"));

        supportingFiles.add(new SupportingFile("pom.mustache", "", "pom.xml")
                .doNotOverwrite());
        supportingFiles.add(new SupportingFile("README.mustache", "", "README.md")
                .doNotOverwrite());

        supportingFiles.add(new SupportingFile("package-info-service.mustache", sourceFolder + File.separator + apiPackage.replace(".", File.separator), "package-info.java"));

        if (this.useDataObject) {
            supportingFiles.add(new SupportingFile("package-info-model.mustache", sourceFolder + File.separator + modelPackage.replace(".", File.separator), "package-info.java"));
            supportingFiles.add(new SupportingFile("json-mappers.mustache", resourceFolder  + File.separator + "META-INF/vertx", "json-mappers.properties"));
            supportingFiles.add(new SupportingFile("DataObjectMapper.mustache", sourceFolder + File.separator + modelPackage.replace(".", File.separator), "DataObjectMapper.java"));
        }
    }

    @Override
    public void postProcessModelProperty(CodegenModel model, CodegenProperty property) {
        super.postProcessModelProperty(model, property);
        if (!model.isEnum) {
            model.imports.add("JsonInclude");
            model.imports.add("JsonProperty");
            if (model.hasEnums) {
                model.imports.add("JsonValue");
            }
        }
    }

    @Override
    public OperationsMap postProcessOperationsWithModels(OperationsMap objs, List<ModelMap> allModels) {
        OperationsMap newObjs = super.postProcessOperationsWithModels(objs, allModels);
        OperationMap operations = newObjs.getOperations();
        if (operations != null) {
            List<CodegenOperation> ops = operations.getOperation();
            for (CodegenOperation operation : ops) {
                operation.httpMethod = operation.httpMethod.toLowerCase(Locale.ROOT);

                if ("Void".equalsIgnoreCase(operation.returnType)) {
                    operation.returnType = null;
                }

                if (operation.getHasPathParams()) {
                    operation.path = camelizePath(operation.path);
                }

            }
        }
        return newObjs;
    }

    @Override
    public Map<String, Object> postProcessSupportingFileData(Map<String, Object> objs) {
        generateJSONSpecFile(objs);
        return super.postProcessSupportingFileData(objs);
    }


    @Override
    public CodegenOperation fromOperation(String path, String httpMethod, Operation operation, List<Server> servers) {
        CodegenOperation codegenOperation =
                super.fromOperation(path, httpMethod, operation, servers);
        codegenOperation.imports.add("MainApiException");
        return codegenOperation;
    }

    @Override
    public CodegenModel fromModel(String name, Schema model) {
        CodegenModel codegenModel = super.fromModel(name, model);
        codegenModel.imports.remove("ApiModel");
        codegenModel.imports.remove("ApiModelProperty");
        return codegenModel;
    }

    @Override
    public void preprocessOpenAPI(OpenAPI openAPI) {
        super.preprocessOpenAPI(openAPI);

        // add server port from the swagger file, 8080 by default
        URL url = URLPathUtils.getServerURL(openAPI, serverVariableOverrides());
        this.additionalProperties.put("serverPort", URLPathUtils.getPort(url, 8080));

        // retrieve api version from swagger file, 1.0.0-SNAPSHOT by default
        // set in super.preprocessOpenAPI
        /*
        if (openAPI.getInfo() != null && openAPI.getInfo().getVersion() != null) {
            artifactVersion = apiVersion = openAPI.getInfo().getVersion();
        } else {
            artifactVersion = apiVersion;
        }*/

        /*
         * manage operation & custom serviceId because operationId field is not
         * required and may be empty
         */
        Map<String, PathItem> paths = openAPI.getPaths();
        if (paths != null) {
            for (Entry<String, PathItem> entry : paths.entrySet()) {
                manageOperationNames(entry.getValue(), entry.getKey());
            }
        }
        this.additionalProperties.remove("gson");
    }

    private void manageOperationNames(PathItem path, String pathname) {
        String serviceIdTemp;

        Map<HttpMethod, Operation> operationMap = path.readOperationsMap();
        if (operationMap != null) {
            for (Entry<HttpMethod, Operation> entry : operationMap.entrySet()) {
                serviceIdTemp = computeServiceId(pathname, entry);
                entry.getValue().addExtension("x-serviceid", serviceIdTemp);
                entry.getValue().addExtension("x-serviceid-varname",
                        serviceIdTemp.toUpperCase(Locale.ROOT) + "_SERVICE_ID");
            }
        }
    }

    private String computeServiceId(String pathname, Entry<HttpMethod, Operation> entry) {
        String operationId = entry.getValue().getOperationId();
        return (operationId != null) ? operationId
                : entry.getKey().name()
                + pathname.replaceAll("-", "_").replaceAll("/", "_").replaceAll("[{}]", "");
    }

    protected String extractPortFromHost(String host) {
        if (host != null) {
            int portSeparatorIndex = host.indexOf(':');
            if (portSeparatorIndex >= 0 && portSeparatorIndex + 1 < host.length()) {
                return host.substring(portSeparatorIndex + 1);
            }
        }
        return "8080";
    }

    private String camelizePath(String path) {
        String word = path;
        Pattern pattern = Pattern.compile("\\{([^/]*)\\}");
        Matcher matcher = pattern.matcher(word);
        while (matcher.find()) {
            word = matcher.replaceFirst(":" + matcher.group(1));
            matcher = pattern.matcher(word);
        }
        pattern = Pattern.compile("(_)(.)");
        matcher = pattern.matcher(word);
        while (matcher.find()) {
            word = matcher.replaceFirst(matcher.group(2).toUpperCase(Locale.ROOT));
            matcher = pattern.matcher(word);
        }
        return word;
    }

}
