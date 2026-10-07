import React from "react";
import ReactDOM from "react-dom/client";
import { WorkbenchApp } from "./WorkbenchApp";
import "./styles.css";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <WorkbenchApp />
  </React.StrictMode>,
);
