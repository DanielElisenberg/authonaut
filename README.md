# Authonaut

An auth service implemented with kotlin and the micronaut framework.

## Purpose
Built this as a fun side project to:
- Freshen up on my JVM development skills
  - Build a service with [kotlin](https://kotlinlang.org/)
  - Try out [the micronaut framework](https://micronaut.io/)
  - Try out [the thymeleaf templating engine](https://www.thymeleaf.org/)
- Make an application with [htmx](https://htmx.org) and [bulma](https://bulma.io/)
- Implement and understand auth better

## TODO
- [x] ~~Login~~
- [x] ~~2FA~~
- [x] ~~Edit user info~~
- [x] ~~User management~~
- [x] ~~Login history~~
- [x] ~~Redirect functionality~~
- [x] ~~Redirect url management~~
- [ ] Htmx error snackbars
- [ ] Jwks endpoint
- [ ] Better redirect url / client management
- [ ] Refresh token
- [ ] Write some tests with kotest
- [ ] Dockerize

## Thoughts

### Kotlin
Kotlin’s packed with cool features and is generally fun to write. The syntax feels way cleaner than Java. I was a bit let down by the null-safety, especially when you’re stuck using Java libraries that don't have it. Not having to declare thrown errors in method signatures like with Java just makes the error handling feel more like JavaScript or Python. Not sure I would crown Kotlin the best language on the JVM. Would have to refresh my Java and Scala before having an opinion here.
Gradle still feels like JVM build tools/package managers have always felt to me. Meh.

### Micronaut
Honestly, really cool framework. Felt easy to get into, and ecosystem seems super rich. I was expecting to feel heavy and bogged down getting back into JVM development, but this felt pretty lightweight and nice. Maybe I used it wrong, hehe. Would use it again for API development in a JVM language.

### Thymeleaf
Has a lot of features. Felt frustrating compared to other templating engines I have used. Would not like to use it again.

### Htmx
Feels super fun to use. Doesn't really replace React/Sveltekit in my mind, but perfect for building out smaller apps that don't require full on javascript frameworks. To be honest, I am sure htmx can handle some pretty big projects too, if the project is the right fit. Would happily use again.

## Bulma
Haven't used bulma in a long time, and it is still nice. Doesn't give the same "hallelujah"-experience like tailwind does, but then again, it doesn't require any building. Good fit together with htmx, would love to find an alternative that gets me even more excited. Would use again.
