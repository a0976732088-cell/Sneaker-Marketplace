import httpClient from "./httpClient";

export function getBrands() {
  return httpClient.get("/brands");
}