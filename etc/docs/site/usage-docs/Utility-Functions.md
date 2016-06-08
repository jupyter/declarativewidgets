#### Urth.whenReady

Declarative Widgets initialization and the import of web components is
performed asynchronously. This can present timing issues in cells that expect the API of a web component to be available upon execution of the cell. In order to ensure that the required
API is available, make use of `Urth.whenReady(function)`. This API will invoke
the specified function only after pre-requisites have been satisfied. The example
code below demonstrates how to safely access the API of the custom
`urth-core-channel` element:

```
%%html
<urth-core-channel name="mine" id="mychannel"></urth-core-channel>
```

```
%%javascript
var channel = document.getElementById('mychannel');

Urth.whenReady(function() {
    channel.set('myvar', 'myvalue');
});
```