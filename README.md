# nukr

A prototype of social network built using functional programming techniques and features.

## Running

To run the server, just execute:

```
$ lein run [port]
```

The argument `port` is optional. If you don't provide one, the system will be bound to port 4000 by default.

Note that currently the system uses an in-memory storage adapter, which means there isn't any kind of persistence. Every data eventually inserted into the system will be vanished by the time its execution is stopped.

## Schemas

Below are described in structural and semantical ways the problem main entities.

**Profile**

| Param | Spec |
| ----- | ---- |
| :profile :name | `string`<br/>The person's name. |
| :profile :email | `string`<br>The profile email address of the format `address@domain.com[.br]` |
| :profile :gender | `string`<br/>The profile gender. It may be: `"male"`, `"female"` or `"other"`. |
| :profile :password-hash | `string`<br/>The profile's password encrypted. Note passwords, when not encrypted, must be: <ul><li>between 6 and 32 characters long<li>contain at least one uppercase character</li></li><li>include at include at least one of the following: `!@#$%&*;?<>\`</li> |
| :profile :private | `boolean`<br/>Whether a profile has opt-out to be hidden from suggestions or not.<br/>Here, `true` means a profile must be hidden. |
| :profile :connections | `array(string)`<br/>A list of UUIDs, representing the profiles to which a given profile is connected. |


**Suggestion**

| Param | Spec |
| ----- | ---- |
| :suggestion :profile | `Profile`<br/>The profile identified as suggestion. |
| :suggestion :relevance | `int`<br>How many connections this profile recomendation has in common with requested profile. |

## Using

The application features some API endpoints that can be used to interact with it. The following interactions are available.

> Note that all requests must contains the header: 
> `"content-type": "application/json"`

### Creating a new profile

**Request**
```
POST /profiles
```

The request must also include the body as according:
```json
{
  "profile" : {
    "name" : "string",
    "email" : "string",
    "password" : "string",
    "gender" : "male|female|other",
  }
}
```

**Response**

| Status | Description |
| ------ | ----- |
| 201 | Profile created successfully. This response also includes a body containing the just-created profile, according to the aforementioned schema. |
| 400 | No profile was created, because one of the request body params are not following the schema. |
| 405 | The request method used is not POST. |

------------------------------------
### Opting a profile in/out of the suggestions engine

**Request**
```
POST /profiles/:uuid/opt/:private
```

| Param | Value |
| ----- | ----- |
| `uuid` | `string`. The UUID of the profile to opt. |
| `private` | `boolean`<br/>Note that `true` here stands for a profile being out of the suggestions engine. |

**Response**

| Status | Description |
| ------ | ----- |
| 200 | The profile's privacy was set to match `private` param.<br/>This status also features a body with the updated profile serialized. |
| 404 | The `uuid` param provided doesn't correspond to a profile in the database. |

------------------------------------
### Tag two profiles as connected

**Request**
```
POST /profiles/connect/:uuid-a/:uuid-b
```

| Param | Value |
| ----- | ----- |
| `uuid-a` | `string`<br/>The UUID of the first profile |
| `uuid-b` | `string`<br/>The UUID of the second profile |

**Response**

| Status | Description |
| ------ | ----- |
| 200 | Both profiles are now connected. No further body is returned. |
| 404 | Either `uuid-a` or `uuid-b` params provided doesn't correspond to a profile in the database. |

------------------------------------
### Get a list of profiles suggestions

**Request**
```
GET /profiles/:uuid/suggestions[?count]
```

| Param | Value |
| ----- | ----- |
| `uuid` | `string`<br/>The UUID of the profile to which the suggestions must be made. |
| `count` | `int`<br/>Optional. The number of recommendations that must be returned. |

**Response**

| Status | Description |
| ------ | ----- |
| 200 | An array of suggestions objects - according its related schema - is returned in the body of the response.<br/>If `count` param is provided, this array will have at most `count` items.  |
| 404 | The `uuid` param provided doesn't correspond to a profile in the database. |

## Testing

To execute the test suit provided with the application, just run:
```
$ lein test
```
