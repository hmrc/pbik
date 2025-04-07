
# pbik

This microservice retrieves and writes data from/to a `HoD` system called `NPS` via HIP, for updating benefits/ expenses and
excluding/ rescinding individual employees. The frontend service this interacts with is pbik-frontend.

### Running

##### To run this Service you will need:

1) [Service Manager 2](https://github.com/hmrc/sm2) installed
2) [SBT](https://www.scala-sbt.org) Version `>=1.x` installed

### API

API is defined [here](https://github.com/hmrc/pbik/blob/main/conf/app.routes)

### Configuration

This service requires configuration for other services, for example `HIP` requires:

| *Key*                                    | *Description*                   |
|------------------------------------------|---------------------------------|
| `microservice.services.nps.hip.protocol` | The protocol of the HIP service |
| `microservice.services.nps.hip.host`     | The host of the HIP service     |
| `microservice.services.nps.hip.port`     | The port of the HIP service     |

#### Starting the application:

Launch services using `sm2 --start PBIK_ALL`

If you want to run it locally:

- `sm2 --stop PBIK`
- `sbt run`

This application runs on port 9583.

### Testing

Run `./run_all_tests.sh`. This also runs scalafmt and does coverage testing.

or `sbt test` to run the tests only.

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
