package org.openapitools.codegen.java.vertx;

import static java.util.stream.Collectors.groupingBy;
import static org.openapitools.codegen.languages.features.VertxFeatures.USE_DATAOBJECT_OPTION;
import static org.openapitools.codegen.languages.features.VertxFeatures.USE_FUTURE_OPTION;
import static org.openapitools.codegen.languages.features.VertxFeatures.VERTX_VERSION;
import static org.testng.Assert.assertEquals;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.ParseOptions;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.languages.JavaVertXServerCodegen;
import org.testng.annotations.Test;

public class VertxCodegenTest {

  @Test
  public void clientOptsUnicity() {
    JavaVertXServerCodegen codegen = new JavaVertXServerCodegen();
    codegen.cliOptions()
      .stream()
      .collect(groupingBy(CliOption::getOpt))
      .forEach((k, v) -> assertEquals(v.size(), 1, k + " is described multiple times"));
  }

  @Test
  public void doAnnotateDatesOnModelParameters() throws IOException {
    File output;
    if (!Files.exists(Path.of("..","..","samples","server","petstore","java-vertx"))) {
      output = Files.createDirectory(Path.of("..","..","samples","server","petstore","java-vertx")).toFile().getCanonicalFile();
    } else {
      output = Path.of("..","..","samples","server","petstore","java-vertx").toFile().getCanonicalFile();
    }
    // output.deleteOnExit();

    OpenAPI openAPI = new OpenAPIParser()
      .readLocation("src/test/resources/3_0/petstore.yaml", null, new ParseOptions()).getOpenAPI();

    JavaVertXServerCodegen codegen = new JavaVertXServerCodegen();
    codegen.setOutputDir(output.getAbsolutePath());

    codegen.additionalProperties().put(USE_DATAOBJECT_OPTION, "true");
    codegen.additionalProperties().put(USE_FUTURE_OPTION, "true");
    codegen.additionalProperties().put(VERTX_VERSION, "5.0.8");

    ClientOptInput input = new ClientOptInput();
    input.openAPI(openAPI);
    input.config(codegen);

    DefaultGenerator generator = new DefaultGenerator();

    generator.setGeneratorPropertyDefault(CodegenConstants.MODELS, "true");
    generator.setGeneratorPropertyDefault(CodegenConstants.MODEL_TESTS, "false");
    generator.setGeneratorPropertyDefault(CodegenConstants.MODEL_DOCS, "false");
    generator.setGeneratorPropertyDefault(CodegenConstants.APIS, "true");
    generator.setGeneratorPropertyDefault(CodegenConstants.SUPPORTING_FILES, "true");
    generator.setGenerateMetadata(false);
    generator.opts(input).generate();
  }

}
