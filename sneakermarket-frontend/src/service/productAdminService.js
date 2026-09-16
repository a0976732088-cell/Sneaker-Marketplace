import httpClient from "./httpClient";

export function getAdminProducts(params = {}) {
  return httpClient.get("/admin/products", { params });
}

export function getAdminProductDetail(productId) {
  return httpClient.get("/products/" + productId);
}

export function createProduct(data) {
  return httpClient.post("/admin/products", data);
}

export function updateProduct(productId, data) {
  return httpClient.put("/admin/products/" + productId, data);
}

export function uploadProductImage(file) {
  const formData = new FormData();
  formData.append("file", file);
  return httpClient.post("/admin/products/upload-image", formData);
}