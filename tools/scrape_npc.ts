import { Glob } from "bun";
import { parseArgs } from "util";
import { join, basename } from "node:path";

const { values, positionals } = parseArgs({
  args: Bun.argv.slice(2),
  allowPositionals: true,
});

const assetsDirectoryPath = positionals[0] ?? undefined;
if (!assetsDirectoryPath) {
  console.error("Make sure to provide a path to the assets folder");
}

const npcDirectory = join(assetsDirectoryPath!, "Server/NPC");

const globJsonFiles = new Glob("**/*.json");
const scannedFiles = Array.from(
  globJsonFiles.scanSync({ cwd: join(npcDirectory, "Roles") }),
);
const mobFiles = scannedFiles.filter((path) => {
  const lp = path.toLowerCase();
  return (
    !lp.includes("test") &&
    !lp.includes("_core") &&
    !lp.includes("component_") &&
    !lp.includes("template_")
  );
});

const distinctMaxHealth = new Map<number, string[]>();
for (const mobPath of mobFiles) {
  const mobFilePath = join(npcDirectory, "Roles", mobPath);
  const mobFile = Bun.file(mobFilePath);
  const mobFilename = basename(mobFilePath, ".json");
  const mobData = (await mobFile.text()).replace(/[ \t\r\n]*/g, "");
  const maxHealthMatch = mobData.match(/"MaxHealth":([0-9]*),/);
  const maxHealth = maxHealthMatch?.[1]
    ? parseInt(maxHealthMatch[1])
    : undefined;
  const baseDamageMatch = mobData.match(
    /"BaseDamage":\{"([a-zA-Z]*)":([0-9]*)\}/,
  );
  // console.log(
  //   mobFilename,
  //   maxHealth,
  //   baseDamageMatch?.[1],
  //   baseDamageMatch?.[2],
  // );
  if (maxHealth) {
    if (distinctMaxHealth.has(maxHealth)) {
      distinctMaxHealth.get(maxHealth)!.push(mobFilename);
    } else {
      distinctMaxHealth.set(maxHealth, [mobFilename]);
    }
  }
}
// console.log(distinctMaxHealth);
console.log(
  "distinctMaxHealth",
  Array.from(distinctMaxHealth.keys()).sort((a, b) =>
    a < b ? -1 : a > b ? 1 : 0,
  ),
);
