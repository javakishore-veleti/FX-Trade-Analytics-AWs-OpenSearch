const { useState } = React;

function Sidebar({ onSelect }) {
  const endpoints = {
    "Trade Service": [
      { name: "Create Trade", path: "http://localhost:9080/trades", method: "POST" },
      { name: "Search by Risk", path: "http://localhost:9080/trades/search/risk", method: "GET" }
    ]
  };

  return (
    React.createElement("div", { style: { width: 250 } },
      Object.entries(endpoints).map(([service, apis]) =>
        React.createElement("div", { key: service },
          React.createElement("h4", null, service),
          apis.map(api =>
            React.createElement("div", {
              key: api.name,
              onClick: () => onSelect(api),
              style: { cursor: "pointer", marginBottom: 5 }
            }, api.name)
          )
        )
      )
    )
  );
}

function App() {
  const [endpoint, setEndpoint] = useState(null);
  const [response, setResponse] = useState([]);
  const [form, setForm] = useState({});

  const submit = async () => {
    let res;
    if (endpoint.method === "POST") {
      res = await axios.post(endpoint.path, form);
      alert(JSON.stringify(res.data));
    } else {
      res = await axios.get(endpoint.path, { params: form });
      setResponse(res.data.hits?.hits || []);
    }
  };

  return (
    React.createElement("div", { style: { display: "flex" } },
      React.createElement(Sidebar, { onSelect: setEndpoint }),
      React.createElement("div", { style: { padding: 20, flex: 1 } },
        endpoint && React.createElement("div", null,
          React.createElement("h3", null, endpoint.name),
          React.createElement("input", {
            placeholder: "risk",
            onChange: e => setForm({ ...form, risk: e.target.value })
          }),
          React.createElement("button", { onClick: submit }, "Submit")
        ),

        React.createElement("table", { border: 1 },
          React.createElement("tbody", null,
            response.map((row, i) =>
              React.createElement("tr", { key: i },
                Object.values(row._source || row).map((v, j) =>
                  React.createElement("td", { key: j }, v)
                )
              )
            )
          )
        )
      )
    )
  );
}

ReactDOM.createRoot(document.getElementById("root")).render(React.createElement(App));
