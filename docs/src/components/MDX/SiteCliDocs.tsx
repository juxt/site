import {parseEDNString} from 'edn-data';

type ArgDoc = [string, string, string];

export function SiteCliArgs({argString}: {argString: string}) {
  const args = parseEDNString(argString) as ArgDoc[] | null;

  return (
    <div>
      {args ? (
        <>
          <h2>Arguments</h2>
          <table className="table-auto w-full">
            <thead>
              <tr>
                <th className="px-4 py-2">Shorthand Flag</th>
                <th className="px-4 py-2">Longhand Flag</th>
                <th className="px-4 py-2">Description</th>
              </tr>
            </thead>
            <tbody>
              {args.map(([shorthand, longhand, description]) => (
                <tr key={shorthand}>
                  <td className="border px-4 py-2">{shorthand}</td>
                  <td className="border px-4 py-2">{longhand}</td>
                  <td className="border px-4 py-2">{description}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      ) : (
        <p>No arguments</p>
      )}
    </div>
  );
}
/*
  const temp = `
  [["config"
    {:description (:doc (meta 'show-config))
     :cli-options []
     :delegate show-config}]

   ["get-token"
    {:description (:doc (meta 'get-token))
     :cli-options [["-u" "--username USERNAME" "Username"]
                   ["-p" "--password PASSWORD" "Password"]
                   ["-K" "--curl FILE" "Store creds in curl config file"]]
     :delegate get-token}]

   ["check-token"
    {:description (:doc (meta 'check-token))
     :cli-options []
     :delegate check-token}]

   ["clear-token"
    {:description (:doc (meta 'clear-token))
     :cli-options []
     :delegate clear-token}]

   ["list-users"
    {:description (:doc (meta 'list-users))
     :cli-options []
     :delegate list-users}]

   ["put-user"
    {:description (:doc (meta 'put-user))
     :cli-options [["-n" "--name NAME" "The user's name"]
                   ["-u" "--username USERNAME" "The username of the user you are creating/updating"]
                   ["-p" "--password PASSWORD" "A temporary password for the user"]
                   ["-e" "--email EMAIL" "The user's email"]]
     :delegate put-user}]

   ["reset-password"
    {:description (:doc (meta 'reset-password))

     :cli-options [["-u" "--username USERNAME" "The username of the user"]
                   ["-p" "--password PASSWORD" "A temporary password for the user"]]
     :delegate reset-password}]

   ["put-role"
    {:description (:doc (meta 'put-role))
     :cli-options [["-r" "--role ROLE" "The role you are creating/updating"]
                   ["-d" "--description DESCRIPTION" "An optional description"]]
     :delegate put-role}]

   ["list-roles"
    {:description (:doc (meta 'list-roles))
     :cli-options []
     :delegate list-roles}]

   ["assign-role"
    {:description (:doc (meta 'assign-role))
     :cli-options [["-u" "--username USERNAME" "The user receiving the role"]
                   ["-r" "--role ROLE" "The role you are assigning"]
                   ["-j" "--justification JUSTIFICATION" "The business justification"]]
     :delegate assign-role}]

   ["put-rule"
    {:description (:doc (meta 'put-rule))
     :cli-options [["-n" "--name NAME" "Rule name"]
                   ["-r" "--rule FILE" "Rule file"]]
     :delegate put-rule}]

   ["put-trigger"
    {:description (:doc (meta 'put-trigger))
     :cli-options [["-n" "--name NAME" "Trigger name"]
                   ["-t" "--trigger FILE" "Trigger file"]]
     :delegate put-trigger}]

   ["put-graphql"
    {:description (:doc (meta 'put-graphql))
     :cli-options [["-f" "--file FILE" "GraphQL schema file"]
                   ["-p" "--path PATH" "The destination path after the base-uri"]]
     :delegate put-graphql}]

   ["post-graphql"
    {:description (:doc (meta 'post-graphql))
     :cli-options [["-f" "--file FILE" "GraphQL request"]
                   ["-p" "--path PATH" "The destination path after the base-uri"]]
     :delegate post-graphql}]

   ["put-redirect"
    {:description (:doc (meta 'put-redirect))
     :cli-options [["-r" "--resource URL" "Resource"]
                   ["-l" "--location URL" "Location to redirect to"]]
     :delegate put-redirect}]

   ["put-api"
    {:description (:doc (meta 'put-api))
     :cli-options [["-n" "--name NAME" "API name"]
                   ["-f" "--openapi FILE" "OpenAPI description file"]]
     :delegate put-api}]

   ["put-asset"
    {:description (:doc (meta 'put-asset))
     :cli-options [["-f" "--file FILE" "The asset file"]
                   ["-p" "--path PATH" "The destination path after the base-uri"]
                   ["-t" "--type MIME_TYPE" "The content-type"]
                   ["-e" "--encoding ENCODING" "The content-encoding (optional)"]
                   ["-l" "--language LANGUAGE" "The content-language (optional)"]
                   ["-r" "--resource URI" "The resource for which this asset is a variant of"]
                   ["-c" "--classification CLASSIFICATION" "The classification (e.g PUBLIC, RESTRICTED) applied to resource"]]
     :delegate put-asset}]

   ["put-template"
    {:description (:doc (meta 'put-template))
     :cli-options [["-f" "--file FILE" "The template file"]
                   ["-p" "--path PATH" "The destination path after the base-uri"]
                   ["-t" "--type MIME_TYPE" "The content-type"]
                   ["-e" "--encoding ENCODING" "The content-encoding (optional)"]
                   ["-l" "--language LANGUAGE" "The content-language (optional)"]
                   ["-d" "--dialect DIALECT" "The template dialect (e.g. selmer, mustache)"]]
     :delegate put-asset}]

   ["post-resources"
    {:description (:doc (meta 'post-resources))
     :cli-options [["-f" "--file FILE" "Resource file (in EDN format)"]]
     :delegate post-resources}]

   ["post"
    {:description (:doc (meta 'post))
     :cli-options
     [ ;; One of these
      ["-p" "--path PATH" "The destination path after the base-uri"]
      ["-u" "--url URL" "URL to post to"]

      ["-t" "--type MIME_TYPE" "The content-type"] ; If missing, try to deduce
                                                   ; from other the file suffix
      ["-e" "--encoding ENCODING" "The content-encoding (optional)"]
      ["-l" "--language LANGUAGE" "The content-language (optional)"]

      ;; One of these
      ["-d" "--data DATA" "Data to post"]
      ["-f" "--file FILE" "File containing data"]]
     :delegate post}]

   ["post-json"
    {:description (:doc (meta 'post-json))
     :cli-options [["-f" "--file FILE" "File containing data"]
                   ["-u" "--url URL" "URL to POST to"]]
     :delegate post-json}]

   ["put-json"
    {:description (:doc (meta 'put-json))
     :cli-options [["-f" "--file FILE" "File containing data"]
                   ["-u" "--url URL" "URL to POST to"]]
     :delegate put-json}]

   ["put-static-site"
    {:description (:doc (meta 'put-static-site))
     :cli-options [["-d" "--directory DIR" "Directory containing static site files"]
                   ["-p" "--path PATH" "Path the site will be available at. e.g -p mysite will make the site available at BASE_URI/mysite/index.html"]
                   ["-spa" "--spa SPA" "If set the html file will be served at PATH/app/*"]]
     :delegate put-static-site}]]
  `;

  const data = parseEDNString(temp) as string[][];
  console.log(
    data.map(([name]) => {
      return {
        title: name,
        path: `/reference/cli/${name}`,
      };
    })
  );
  */
