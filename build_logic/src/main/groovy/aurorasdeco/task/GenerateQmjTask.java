package aurorasdeco.task;

import aurorasdeco.Constants;
import aurorasdeco.extension.AurorasDecoExtension;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.quiltmc.json5.JsonWriter;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class GenerateQmjTask extends DefaultTask {
	@OutputDirectory
	public abstract DirectoryProperty getOutputDir();

	@Nested
	public abstract Property<AurorasDecoExtension> getAurorasDecoModule();

	@Inject
	public GenerateQmjTask() {
		this.setGroup("generation");
	}

	@TaskAction
	public void generateQmj() throws IOException {
		Path output = this.getOutputDir().getAsFile().get().toPath().resolve("fabric.mod.json");
		this.getProject().getLogger().lifecycle(output.toAbsolutePath().toString());

		if (Files.exists(output)) {
			Files.delete(output);
		}

		JsonWriter writer = JsonWriter.json(output);

		writer.beginObject()
				.name("schemaVersion").value(1)
				.name("id").value(Constants.NAMESPACE)
				.name("version").value(this.getProject().getVersion().toString())
				.name("name").value(Constants.NAME)
				.name("description").value(Constants.DESCRIPTION)
				.name("authors").beginArray();

		for (var entry : Constants.CONTRIBUTORS) {
			if ("Author".equals(entry.role())) {
				writer.value(entry.name());
			}
		}
		
		writer.endArray()
				.name("contributors").beginArray();

		for (var entry : Constants.CONTRIBUTORS) {
			if (!("Author".equals(entry.role()))) {
				writer.value(entry.name());
			}
		}
		
		writer.endArray()
				.name("contact").beginObject();
		
		{
			writer.name("homepage").value(Constants.Links.WEBSITE)
					.name("sources").value(Constants.Links.SOURCES)
					.name("issues").value(Constants.Links.ISSUES);
		}
		
		writer.endObject()
				.name("license").value(Constants.LICENSE)
				.name("icon").value(Constants.ICON_PATH);
		
		{
			//writer.name("intermediate_mappings").value("net.fabricmc:intermediary");
			writer.name("environment").value("*");

			if (!this.getAurorasDecoModule().get().getEntrypoints().isEmpty()) {
				writer.name("entrypoints").beginObject();

				for (var entrypoint : this.getAurorasDecoModule().get().getEntrypoints()) {
					if (!entrypoint.getEnabled().get()) continue;

					writer.name(entrypoint.getName());
					writer.beginArray();
					for (var target : entrypoint.getValues().get()) {
						target.write(writer);
					}
					writer.endArray();
				}

				writer.endObject();
			}

			writer.name("depends").beginObject();
			{
				writer.name("minecraft");
				if (Constants.MINECRAFT_VERSION.supported().isEmpty()) {
					writer.value(Constants.MINECRAFT_VERSION.version());
				} else {
					writer.beginArray();
					for (var version : Constants.MINECRAFT_VERSION.all()) {
						writer.value("=" + version);
					}
					writer.endArray();
				}

				writer.name("quilt_loader").value(">=" + Constants.LOADER_VERSION);
				writer.name("quilted_fabric_api").value(">=" + Constants.QFAPI_VERSION);
				writer.name("java").value(">=" + Constants.JAVA_VERSION);
				writer.name("terraform-wood-api-v1").value(">=" + Constants.TERRAFORM_WOOD_API_VERSION);
			}
			writer.endObject();
		}

		writer.name("mixins").beginArray().value("aurorasdeco.mixins.json").endArray();

		/*writer.name("modmenu").beginObject();
		{
			writer.name("links").beginObject();
			{
				writer.name("modmenu.curseforge").value(Constants.Links.CURSEFORGE)
						.name("modmenu.discord").value(Constants.Links.DISCORD)
						.name("modmenu.github_releases").value(Constants.Links.GITHUB_RELEASES)
						.name("modmenu.modrinth").value(Constants.Links.MODRINTH)
						.name("modmenu.twitter").value(Constants.Links.TWITTER);
			}
			writer.endObject();
		}
		writer.endObject();*/

		writer.endObject();
		writer.flush();
		writer.close();
	}
}
