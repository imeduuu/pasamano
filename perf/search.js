import http from 'k6/http';
import { check } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://host.docker.internal:8080';

export const options = {
  vus: 50,
  duration: '1m',
  thresholds: {
    http_req_duration: ['p(95)<500'], // RNF-REN-02
    http_req_failed: ['rate<0.01'],
  },
};

const searches = [
  '?q=celular&sort=relevance&page=0',
  '?q=bicicleta&condition=USADO&sort=price_asc&page=2',
  '?q=hogar&minPrice=10000&maxPrice=500000&sort=price_desc&page=4',
  '?region=Metropolitana%20de%20Santiago&condition=NUEVO&sort=recent&page=10',
  '?q=computacion&minPrice=50000&condition=NUEVO&sort=recent&page=1',
  '?minPrice=100000&maxPrice=1000000&sort=price_asc&page=20',
  '?q=ropa&sort=relevance&page=3',
  '?region=Valpara%C3%ADso&maxPrice=250000&sort=price_desc&page=5',
];

export default function () {
  const query = searches[Math.floor(Math.random() * searches.length)];
  const response = http.get(`${baseUrl}/api/v1/listings${query}`, {
    tags: { endpoint: 'listing_search' },
  });
  check(response, {
    'search responds 200': (r) => r.status === 200,
    'search response is JSON': (r) => r.headers['Content-Type']?.includes('application/json'),
  });
}
